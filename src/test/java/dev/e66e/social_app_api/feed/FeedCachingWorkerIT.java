package dev.e66e.social_app_api.feed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.modulith.test.ModuleSlicing;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@DataJpaAndRedis
@FeedTestProperties
@Import({
        FeedCachingWorker.class,
        FeedCachingWorkerIT.PropertiesConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ModuleSlicing
public class FeedCachingWorkerIT {

    @Autowired
    FeedCachingWorker worker;

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    ProcessedFeedEventRepository processedRepo;

    @Autowired
    FeedEventFailureRepository failureRepo;

    @Autowired
    DeadLetterFeedEventRepository deadLetterRepo;

    @Autowired
    FeedAsyncProperties aProps;

    @MockitoBean(extraInterfaces = FeedExternalAPI.class)
    FeedInternalAPI feedAPI;

    @TestConfiguration
    @EnableConfigurationProperties({FeedAsyncProperties.class, FeedProperties.class})
    static class PropertiesConfig {}

    @BeforeEach
    void cleanState() {
        RedisConnectionFactory conn = redis.getConnectionFactory();
        if (conn == null) {
            throw new RuntimeException();
        }

        conn.getConnection().serverCommands().flushAll();

        redis.opsForStream().createGroup(aProps.getStream().getKey(),
                ReadOffset.from("0"),
                aProps.getConsumer().getGroup());
        processedRepo.deleteAll();
        failureRepo.deleteAll();
        deadLetterRepo.deleteAll();
    }

    @Test
    void processesNewRecord_successfully_acksAndMarksProcessed() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        publish(eventId, postId);

        // When
        worker.consumePostCreationEvenStream();

        // Then
        verify(feedAPI).processPostCreatedEvent(postId);
        assertThat(processedRepo.findById(eventId)).isPresent();
        assertThat(failureRepo.findById(eventId)).isEmpty();
        assertThat(pendingCount()).isZero();
    }

    @Test
    void duplicateEvent_isAcknowledgedWithoutProcessingAgain() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        processedRepo.saveAndFlush(new ProcessedFeedEvent(eventId));

        publish(eventId, postId);

        // When
        worker.consumePostCreationEvenStream();

        // Then
        verify(feedAPI, never()).processPostCreatedEvent(any());
        assertThat(pendingCount()).isZero();
    }

    @Test
    void malformedRecord_isAcknowledgedAndSkipped() {
        // Given
        redis.opsForStream().add(MapRecord.create(aProps.getStream().getKey(),
                Map.of(
                    "eventId", "not-a-uuid",
                    "postId", "not-a-uuid"
        )));

        // When
        worker.consumePostCreationEvenStream();

        // Then
        verifyNoInteractions(feedAPI);
        assertThat(pendingCount()).isZero();
    }

    @Test
    void failingEvent_belowMaxAttempts_staysPendingWithFailureRecord() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        doThrow(new RuntimeException("post module blew up"))
                .when(feedAPI).processPostCreatedEvent(postId);

        publish(eventId, postId);

        // When
        worker.consumePostCreationEvenStream();

        // Then
        FeedEventFailure failure = failureRepo.findById(eventId).orElseThrow();
        assertThat(failure.getAttemptCount()).isEqualTo(1);
        assertThat(failure.getLastError()).contains("post module blew up");
        assertThat(deadLetterRepo.count()).isZero();
        assertThat(pendingCount()).isEqualTo(1);
    }

    @Test
    void failingEvent_atMaxAttempts_movesToDeadLetterAndAcknowledges() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        doThrow(new RuntimeException("persistent failure"))
                .when(feedAPI).processPostCreatedEvent(postId);

        publish(eventId, postId);

        // When
        worker.consumePostCreationEvenStream();
        worker.recoverPostCreationEventStream();

        // Then
        assertThat(deadLetterRepo.count()).isEqualTo(1);
        DeadLetterFeedEvent dlq = deadLetterRepo.findAll().getFirst();
        assertThat(dlq.getEventId()).isEqualTo(eventId);
        assertThat(dlq.getAttempts()).isEqualTo(2);

        assertThat(failureRepo.findById(eventId)).isEmpty();
        assertThat(pendingCount()).isZero();

        assertThat(redis.opsForStream().size(aProps.getConsumer().getDlqStreamKey())).isEqualTo(1L);
        List<MapRecord<String, Object, Object>> dlqQ =
                redis.opsForStream().range(aProps.getConsumer().getDlqStreamKey(), Range.unbounded());
        assertThat(dlqQ).hasSize(1);
        Map<Object, Object> fields = dlqQ.getFirst().getValue();
        assertThat(fields.get("eventId")).isEqualTo(eventId.toString());
        assertThat(fields.get("sourceRecordId")).isEqualTo(dlq.getStreamRecordId());
        assertThat(fields.get("attemptCount")).isEqualTo("2");
        assertThat((String) fields.get("payloadJson")).contains(eventId.toString());
    }

    @Test
    void successAfterFailure_clearsFailureState() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        doThrow(new RuntimeException("transient"))
                .doNothing()
                .when(feedAPI).processPostCreatedEvent(postId);

        publish(eventId, postId);

        // When
        worker.consumePostCreationEvenStream();
        worker.recoverPostCreationEventStream();

        // Then
        assertThat(failureRepo.findById(eventId)).isEmpty();
        assertThat(processedRepo.findById(eventId)).isPresent();
        assertThat(pendingCount()).isZero();
    }

    @Test
    void reclaimStalePending_claimsAndProcesses() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID postId  = UUID.randomUUID();

        redis.opsForStream().add(MapRecord.create(aProps.getStream().getKey(),
                Map.of(
                        "eventId", eventId.toString(),
                        "postId", postId.toString()
                )));

        //noinspection unchecked
        redis.opsForStream().read(
                Consumer.from(aProps.getConsumer().getGroup(), "dead-consumer"),
                StreamReadOptions.empty().count(1),
                StreamOffset.create(aProps.getStream().getKey(),
                        ReadOffset.lastConsumed()));

        assertThat(redis.opsForStream().pending(
                        aProps.getStream().getKey(),
                        aProps.getConsumer().getGroup())
                .getTotalPendingMessages())
                .as("record should be pending before reclaim")
                .isEqualTo(1L);

        // When
        worker.reclaimStalePendingPostEvents();

        // Then
        verify(feedAPI).processPostCreatedEvent(postId);
        assertThat(pendingCount()).isZero();
    }

    @Test
    void recover_retriesUnackedPendingRecord() {
        // Given
        UUID eventId = UUID.randomUUID();
        UUID postId  = UUID.randomUUID();

        doThrow(new RuntimeException("boom"))
                .doNothing()
                .when(feedAPI).processPostCreatedEvent(postId);

        publish(eventId, postId);

        // When
        worker.consumePostCreationEvenStream();   // fails, stays pending

        worker.recoverPostCreationEventStream();  // succeeds

        // Then
        verify(feedAPI, times(2)).processPostCreatedEvent(postId);
        assertThat(processedRepo.findById(eventId)).isPresent();
        assertThat(pendingCount()).isZero();
    }

    // ---- helpers ----------------------------------------------------------------------------------------------------

    private void publish(UUID eventId, UUID postId) {
        redis.opsForStream().add(MapRecord.create(aProps.getStream().getKey(),
                Map.of(
                    "eventId", eventId.toString(),
                    "postId",postId.toString()
        )));
    }

    private long pendingCount() {
        var pending = redis.opsForStream().pending(
                aProps.getStream().getKey(),
                aProps.getConsumer().getGroup());
        return pending == null
                ? 0L
                : pending.getTotalPendingMessages();
    }
}
