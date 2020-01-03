package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.util.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// stomp over websocket

@Controller
public class LiveSubmissionController {

    private SimpMessagingTemplate template;

    @Autowired
    SmoothieSubmissionService submissionService;

    @Autowired
    public LiveSubmissionController(SimpMessagingTemplate template) {
        this.template = template;
    }

    @MessageMapping("/live-submission/{submissionId}")
    public Mono<List<Submission.SubmissionBatchCase>> liveSubmissionRoute(@DestinationVariable String submissionId) {
        return submissionService.findSubmissionById(submissionId)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(s -> {
                    List<Submission.SubmissionBatchCase> l = new ArrayList<>();
                    for (var nl : s.getBatchCases()) l.addAll(nl);
                    return Mono.just(l);
                })
                .onErrorResume(e -> Mono.just(new ArrayList<>()));
    }

    public void sendSubmissionBatch(String submissionId, Submission.SubmissionBatchCase batchCase) {
        template.convertAndSend("/live-submission/" + submissionId, Arrays.asList(batchCase));
    }

}
