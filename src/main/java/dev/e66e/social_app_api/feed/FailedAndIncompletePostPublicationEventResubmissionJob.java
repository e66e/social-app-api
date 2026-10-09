package dev.e66e.social_app_api.feed;

import dev.e66e.social_app_api.posts.PostPublishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.FailedEventPublications;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
class FailedAndIncompletePostPublicationEventResubmissionJob {

    private final FailedEventPublications failedEventPublications;
    private final IncompleteEventPublications incompleteEventPublications;
    private final FeedAsyncProperties prop;

    @Scheduled(fixedDelayString = "${feed.async.publisher.failed.fixed-delay-ms}")
    void resubmitFailedEvent() {
        ResubmissionOptions options = ResubmissionOptions.defaults()
                .withBatchSize(this.prop.getPublisher().getFailed().getBatchSize())
                .withMaxInFlight(this.prop.getPublisher().getFailed().getMaxInFlight())
                .withMinAge(Duration.ofSeconds(5))
                .withFilter(pub -> pub.getEvent()
                        .getClass().equals(PostPublishedEvent.class));

        this.failedEventPublications.resubmit(options);
    }

    @Scheduled(fixedDelayString = "${feed.async.publisher.in-progress.fixed-delay-ms}")
    void resubmitInProgressEvent() {
        ResubmissionOptions options = ResubmissionOptions.defaults()
                        .withMinAge(Duration.ofSeconds(15));

        this.incompleteEventPublications.resubmitIncompletePublications(options);
    }
}
