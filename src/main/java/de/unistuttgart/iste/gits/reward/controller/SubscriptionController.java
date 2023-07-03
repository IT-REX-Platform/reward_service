package de.unistuttgart.iste.gits.reward.controller;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.reward.service.RewardService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final RewardService rewardService;

    /**
     * Event handler for the content-progressed event
     */
    @Topic(name = "content-progressed", pubsubName = "gits")
    @PostMapping(path = "/content-service/resource-update-pubsub")
    public Mono<Void> updateAssociation(@RequestBody(required = false) CloudEvent<UserProgressLogEvent> cloudEvent,
                                        @RequestHeader Map<String, String> headers) {
        return Mono.fromRunnable(() -> rewardService.calculateScoresOnContentWorkedOn(cloudEvent.getData()));
    }
}
