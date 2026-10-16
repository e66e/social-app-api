package dev.e66e.social_app_api.feed;

import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {
        "feed.async.stream.key=feed:stream:it",
        "feed.async.consumer.group=feed-it-group",
        "feed.async.consumer.name=feed-it-consumer",
        "feed.async.consumer.dlq-stream-key=feed:dlq:it",
        "feed.async.consumer.batch-size=10",
        "feed.async.consumer.reclaim-enabled=true",
        "feed.async.consumer.reclaim-fixed-delay-ms=3600000",
        "feed.async.consumer.reclaim-idle-ms=1",
        "feed.async.consumer.reclaim-batch-size=10",
        "feed.async.consumer.fixed-delay-ms=3600000",
        "feed.async.consumer.recover-fixed-delay-ms=3600000",
        "feed.async.consumer.max-processing-attempts=2",
        "feed.async.publisher.failed.fixed-delay-ms=3600000",
        "feed.async.publisher.failed.batch-size=10",
        "feed.async.publisher.failed.max-in-flight=10",
        "feed.async.publisher.in-progress.fixed-delay-ms=3600000",
        "feed.default-page-size=10",
        "feed.home-feed-ttl-in-minutes=60",
        "feed.max-page-size=50"
})
public @interface FeedTestProperties {
}
