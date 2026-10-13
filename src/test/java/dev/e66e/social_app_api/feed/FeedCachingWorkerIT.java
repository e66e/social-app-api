package dev.e66e.social_app_api.feed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;


@DataJpaAndRedis
@FeedTestProperties
@Import({
        FeedCachingWorker.class,
        FeedCachingWorkerIT.PropertiesConfig.class
})
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
        UUID eventId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        publish(eventId, postId);

        worker.consumePostCreationEvenStream();


        verify(feedAPI).processPostCreatedEvent(postId);
        assertThat(processedRepo.findById(eventId)).isPresent();
        assertThat(failureRepo.findById(eventId)).isEmpty();
        assertThat(pendingCount()).isZero();
    }

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
