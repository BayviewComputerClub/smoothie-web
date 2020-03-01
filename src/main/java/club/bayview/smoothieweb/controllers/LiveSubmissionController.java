package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.WebSocketSessionService;
import club.bayview.smoothieweb.util.NotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.util.ArrayList;
import java.util.List;

// stomp over websocket

public class LiveSubmissionController implements WebSocketHandler {

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        SmoothieSubmissionService submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);
        WebSocketSessionService sessionService = SmoothieWebApplication.context.getBean(WebSocketSessionService.class);

        StringBuilder submissionId = new StringBuilder();

        UnicastProcessor<WebSocketMessage> inputStream = UnicastProcessor.create();

        return session.send(inputStream)
                .and(session.receive()
                        .doOnNext(m -> {
                            String payload = m.getPayloadAsText(); // warning: getPayloadAsText actually takes the data from the buffer
                            sessionService.addSession("/live-submission/" + payload, session, inputStream);
                            submissionId.append(payload);

                            submissionService.findSubmissionById(payload)
                                    .switchIfEmpty(Mono.error(new NotFoundException()))
                                    .doOnNext(s -> {
                                        List<Submission.SubmissionBatchCase> l = new ArrayList<>();
                                        for (var nl : s.getBatchCases()) l.addAll(nl);

                                        ObjectMapper mapper = new ObjectMapper();
                                        try {
                                            inputStream.onNext(session.textMessage(mapper.writeValueAsString(l)));
                                        } catch (JsonProcessingException e) {
                                            e.printStackTrace();
                                        }
                                    })
                                    .doOnError(e -> inputStream.onNext(session.textMessage("[]")))
                                    .subscribe();
                        })
                        .doFinally(signalType -> sessionService.removeSession("/live-submission/" + submissionId, session.getId())))
                .then();
    }
}
