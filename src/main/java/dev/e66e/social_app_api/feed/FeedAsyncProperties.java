package dev.e66e.social_app_api.feed;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "feed.async")
class FeedAsyncProperties {

    private final Stream stream = new Stream();
    private final Publisher publisher = new Publisher();
    private final Consumer consumer = new Consumer();

    @Getter @Setter
    public static class Stream {
        private String key;
    }

    @Getter @Setter
    public static class Publisher {

        private final Failed failed = new Failed();
        private final InProgress inProgress = new InProgress();

        @Getter @Setter
        public static class Failed {
            private int batchSize;
            private int fixedDelayMs;
            private int maxInFlight;
        }

        @Getter @Setter
        public static class InProgress {
            private int fixedDelayMs;
        }
    }

    @Getter @Setter
    public static class Consumer {
        private String group;
        private String name;
        private int batchSize;
        private String dlqStreamKey;
        private boolean reclaimEnabled;
        private int reclaimFixedDelayMs;
        private int reclaimIdleMs;
        private int reclaimBatchSize;
        private int fixedDelayMs;
        private int recoverFixedDelayMs;
        private int maxProcessingAttempts;
    }
}
