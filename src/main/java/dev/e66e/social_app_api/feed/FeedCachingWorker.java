package dev.e66e.social_app_api.feed;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
class FeedCachingWorker {

    private final FeedAsyncProperties prop;
    private final StringRedisTemplate redis;
    private final ProcessedFeedEventRepository processedFeedEventRepository;
    private final FeedEventFailureRepository feedEventFailureRepository;
    private final FeedInternalAPI feedAPI;
    private final DeadLetterFeedEventRepository deadLetterFeedEventRepository;
    private final ObjectMapper objectMapper;
    private final FeedCachingWorker self;

    public FeedCachingWorker(FeedAsyncProperties prop,
                             StringRedisTemplate redis,
                             ProcessedFeedEventRepository processedFeedEventRepository,
                             FeedEventFailureRepository feedEventFailureRepository,
                             FeedInternalAPI feedAPI,
                             DeadLetterFeedEventRepository deadLetterFeedEventRepository,
                             ObjectMapper objectMapper,
                             @Lazy FeedCachingWorker self) {
        this.prop = prop;
        this.redis = redis;
        this.processedFeedEventRepository = processedFeedEventRepository;
        this.feedEventFailureRepository = feedEventFailureRepository;
        this.feedAPI = feedAPI;
        this.deadLetterFeedEventRepository = deadLetterFeedEventRepository;
        this.objectMapper = objectMapper;
        this.self = self;
    }

    @PostConstruct
    void initializeConsumerGroup() {
        validateConfiguration();
        String streamKey = prop.getStream().getKey();
        String consumerGroupName = prop.getConsumer().getGroup();

        try {
            StreamInfo.XInfoGroups groups = redis.opsForStream()
                    .groups(streamKey);

            boolean exists = groups.stream().anyMatch(group ->
                    consumerGroupName.equalsIgnoreCase(group.groupName()));

            if (!exists) {
                redis.opsForStream().createGroup(streamKey,
                        ReadOffset.latest(),
                        consumerGroupName);
            }
        } catch (RedisSystemException e) {
            if (this.isMissingStream(e)) {
                createConsumerGroupWithStream(streamKey, consumerGroupName);
                return;
            }
            throw e;
        }
    }

    private boolean isMissingStream(RedisSystemException e) {
        Throwable cause = e.getCause();
        return cause != null
                && cause.getMessage() != null
                && cause.getMessage().contains("ERR no such key");
    }

    private void createConsumerGroupWithStream(String streamKey, String consumerGroupName) {
        RecordId initializeRecordId = redis.opsForStream().add(
                MapRecord.create(streamKey,
                        Map.of("__system__", "stream-init"))
        );

        try {
            redis.opsForStream().createGroup(streamKey, ReadOffset.latest(), consumerGroupName);
        } catch (RedisSystemException e) {
            if (!this.groupAlreadyExists(e)) {
                throw e;
            }
        } finally {
            if (initializeRecordId != null) {
                redis.opsForStream().delete(streamKey, initializeRecordId);
            }
        }
    }

    private boolean groupAlreadyExists(RedisSystemException e) {
        Throwable cause = e.getCause();
        return cause != null
                && cause.getMessage() != null
                && cause.getMessage().contains("BUSYGROUP");
    }

    private void validateConfiguration() {
        this.requireText(prop.getStream().getKey(), "feed.async.stream.key");
        this.requireText(prop.getConsumer().getGroup(), "feed.async.consumer.group");
        this.requireText(prop.getConsumer().getGroup(), "feed.async.consumer.name");
        this.requireText(prop.getConsumer().getDlqStreamKey(), "feed.async.consumer.dlq-stream-key");

        this.requirePositive(prop.getConsumer().getBatchSize(),
                "feed.async.consumer.batch-size");
        this.requirePositive(prop.getConsumer().getReclaimFixedDelayMs(),
                "feed.async.consumer.reclaim-fixed-delay-ms");
        this.requirePositive(prop.getConsumer().getReclaimIdleMs(),
                "feed.async.consumer.reclaim-idle-ms");
        this.requirePositive(prop.getConsumer().getReclaimBatchSize(),
                "feed.async.consumer.reclaim-batch-size");
        this.requirePositive(prop.getConsumer().getFixedDelayMs(),
                "feed.async.consumer.fixed-delay-ms");
        this.requirePositive(prop.getConsumer().getRecoverFixedDelayMs(),
                "feed.async.consumer.recover-fixed-delay-ms");
    }

    @Scheduled(fixedDelayString = "${feed.async.consumer.fixed-delay-ms}")
    @Transactional
    void consumePostCreationEvenStream() {
        List<MapRecord<String, Object, Object>> records;
        try {
            //noinspection unchecked
            records = redis.opsForStream().read(
                    Consumer.from(
                            prop.getConsumer().getGroup(),
                            prop.getConsumer().getName()
                    ),
                    StreamReadOptions.empty().count(prop.getConsumer().getBatchSize()),
                    StreamOffset.create(prop.getStream().getKey(),
                            ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        records.forEach(this::processRecord);
    }

    @Scheduled(fixedDelayString = "${feed.async.consumer.recover-fixed-delay-ms}")
    @Transactional
    void recoverPostCreationEventStream() {
        List<MapRecord<String, Object, Object>> records;

        try {
            //noinspection unchecked
            records = redis.opsForStream().read(
                    Consumer.from(
                            prop.getConsumer().getGroup(),
                            prop.getConsumer().getName()
                    ),
                    StreamReadOptions.empty().count(prop.getConsumer().getBatchSize()),
                    StreamOffset.create(prop.getStream().getKey(),
                            ReadOffset.from("0"))
            );
        } catch (Exception e) {
            return;
        }

        if (records == null || records.isEmpty()) {
            return;
        }

        records.forEach(this::processRecord);
    }

    @Scheduled(fixedDelayString = "${feed.async.consumer.reclaim-fixed-delay-ms}")
    void reclaimStalePendingPostEvents() {
        if (!prop.getConsumer().isReclaimEnabled()) {
            return;
        }
        List<MapRecord<String, Object, Object>> claimRecords = this.claimStalePendingRecords();
        claimRecords.forEach(this::processRecord);
    }

    private List<MapRecord<String, Object, Object>> claimStalePendingRecords() {
        try {
            String streamKey = prop.getStream().getKey();
            String consumerGroup = prop.getConsumer().getGroup();
            String consumerName = prop.getConsumer().getName();
            long reclaimIdleMs = prop.getConsumer().getReclaimIdleMs();
            PendingMessages pendingMessages = redis.opsForStream()
                    .pending(
                            streamKey,
                            consumerGroup,
                            Range.unbounded(),
                            prop.getConsumer().getReclaimBatchSize());

            if (pendingMessages == null || pendingMessages.isEmpty()) {
                return List.of();
            }

            List<RecordId> staleRecordsId = pendingMessages.stream()
                    .filter(this::isClaimedPendingMessage)
                    .map(PendingMessage::getId)
                    .toList();

            if (staleRecordsId.isEmpty()) {
                return List.of();
            }

            return redis.opsForStream().claim(
                    streamKey,
                    consumerGroup,
                    consumerName,
                    Duration.ofMillis(reclaimIdleMs),
                    staleRecordsId.toArray(new RecordId[0])
            );
        } catch (Exception e) {
            log.warn(e.getMessage());
            return List.of();
        }
    }

    private boolean isClaimedPendingMessage(PendingMessage pendingMessage) {
        if (prop.getConsumer().getName().equals(pendingMessage.getConsumerName())) {
            return false;
        }

        return pendingMessage.getElapsedTimeSinceLastDelivery().toMillis()
                >= prop.getConsumer().getReclaimIdleMs();
    }

    private void requirePositive(Number value, String propertyName) {
        if (value.intValue() <= 0) {
            throw new IllegalStateException(propertyName + " must be greater than 0");
        }
    }

    private void requireText(@Nullable String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must not be blank");
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        String streamKey = prop.getStream().getKey();
        String consumerGroup = prop.getConsumer().getGroup();

        Map<Object, Object> eventFields = record.getValue();

        UUID eventId;
        UUID postId;

        try {
            eventId = UUID.fromString((String) eventFields.get("eventId"));
            postId = UUID.fromString((String) eventFields.get("postId"));
        } catch (IllegalArgumentException e) {
            redis.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
            return;
        }

        try {
            if (!self.tryClaim(eventId)) {
                redis.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
                return;
            }

            this.feedAPI.processPostCreatedEvent(postId);
            clearFailureState(eventId);

            redis.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
        } catch (Exception e) {
            int attemptCount = this.recordFailureAttempt(eventId, e);

            if (attemptCount < prop.getConsumer().getMaxProcessingAttempts()) {
                return;
            }

            this.moveToDeadLetter(record, eventFields, eventId, attemptCount, e);
            redis.opsForStream().acknowledge(streamKey, consumerGroup, record.getId());
            this.feedEventFailureRepository.deleteById(eventId);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryClaim(UUID eventId) {
        try {
            this.processedFeedEventRepository.saveAndFlush(new ProcessedFeedEvent(eventId));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    private void moveToDeadLetter(MapRecord<String, Object, Object> record,
                                  Map<Object, Object> eventFields, UUID eventId,
                                  int attemptCount, Exception e) {

        String payloadJson = this.toJson(eventFields);
        String failureReason = this.sanitizeErrorMessage(e);

        this.deadLetterFeedEventRepository.saveAndFlush(
                new DeadLetterFeedEvent(attemptCount, failureReason, payloadJson,
                        record.getId().getValue(), eventId));

        try {
            redis.opsForStream().add(MapRecord.create(
                    prop.getConsumer().getDlqStreamKey(),
                    Map.of(
                            "eventId", eventId,
                            "sourceRecordId", record.getId().getValue(),
                            "failureReason", failureReason,
                            "attemptCount", String.valueOf(attemptCount),
                            "payloadJson", payloadJson
                    )
            ));
        } catch (Exception ex) {
            log.warn(failureReason, ex);
        }
    }

    private String toJson(Map<Object, Object> eventFields) {
        try {
            return this.objectMapper.writeValueAsString(eventFields);
        } catch (Exception e) {
            return "{\"serializationError\": true}";
        }
    }

    private int recordFailureAttempt(UUID eventId, Exception e) {
        FeedEventFailure failure = this.feedEventFailureRepository.findById(eventId)
                .orElseGet(() -> new FeedEventFailure(eventId));

        int attemptCount = failure.recordFailure(this.sanitizeErrorMessage(e));
        this.feedEventFailureRepository.saveAndFlush(failure);
        return attemptCount;
    }

    private String sanitizeErrorMessage(Exception e) {
        String rawMessage = e.getMessage() == null
                ? e.getClass().getSimpleName()
                : e.getMessage();

        return rawMessage.length() > 1000 ? rawMessage.substring(0, 1000) : rawMessage;
    }

    private void clearFailureState(UUID eventId) {
        this.feedEventFailureRepository.deleteById(eventId);
    }
}
