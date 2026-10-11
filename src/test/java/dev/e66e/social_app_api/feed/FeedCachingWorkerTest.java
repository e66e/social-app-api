package dev.e66e.social_app_api.feed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedCachingWorkerTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    @Mock
    private ProcessedFeedEventRepository processedFeedEventRepository;

    @Mock
    private FeedEventFailureRepository feedEventFailureRepository;

    @Mock
    private FeedInternalAPI feedAPI;

    @Mock
    private DeadLetterFeedEventRepository deadLetterFeedEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    private FeedCachingWorker worker;

    @BeforeEach
    void setUp() {
        FeedAsyncProperties prop = new FeedAsyncProperties();
        prop.getStream().setKey("feed:stream");
        prop.getConsumer().setGroup("feed-group");
        prop.getConsumer().setName("feed-consumer");
        prop.getConsumer().setDlqStreamKey("feed:dlq");
        prop.getConsumer().setMaxProcessingAttempts(2);

        lenient().when(redis.opsForStream()).thenReturn(streamOps);

        worker = new FeedCachingWorker(
                prop,
                redis,
                processedFeedEventRepository,
                feedEventFailureRepository,
                feedAPI,
                deadLetterFeedEventRepository,
                objectMapper
        );
    }

    @Nested
    class ProcessRecordTests {

        @Test
        void success_acknowledgesAndClearsFailure() {
            UUID eventId = UUID.randomUUID();
            UUID postId = UUID.randomUUID();
            MapRecord<String, Object, Object> record = record(eventId, postId);

            when(processedFeedEventRepository.saveAndFlush(any(ProcessedFeedEvent.class)))
                    .thenReturn(new ProcessedFeedEvent(eventId));

            ReflectionTestUtils.invokeMethod(worker, "processRecord", record);

            verify(feedAPI).processPostCreatedEvent(postId);
            verify(feedEventFailureRepository).deleteById(eventId);
            verify(streamOps).acknowledge("feed:stream", "feed-group", record.getId());
        }
    }

    private MapRecord<String, Object, Object> record(UUID eventId, UUID postId) {
        return MapRecord.create(
                "feed:stream",
                Map.of(
                        "eventId", eventId.toString(),
                        "postId", postId.toString()
                )
        );
    }
}