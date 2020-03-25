package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import club.bayview.smoothieweb.services.WebSocketSessionService;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.SessionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.util.ArrayList;
import java.util.List;

// stomp over websocket

public class LiveSubmissionController implements WebSocketHandler {

//    @Getter
//    @Setter
//    public static class LiveSubmissionData {
//        String compileError;
//        Submission.SubmissionStatus status;
//        List<Submission.SubmissionBatchCase> batchCases;
//
//        public static LiveSubmissionData fromSubmission(Submission s) {
//
//        }
//    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        SmoothieSubmissionService submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);
        SmoothieProblemService problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);
        WebSocketSessionService sessionService = SmoothieWebApplication.context.getBean(WebSocketSessionService.class);

        StringBuilder submissionId = new StringBuilder();
        UnicastProcessor<WebSocketMessage> inputStream = UnicastProcessor.create(); // multiplex all input into one publisher

        return sessionService.setupInput(session, inputStream, sessionService.takeOneMsg(session)
                .doOnNext(m -> submissionId.append(m.getPayloadAsText())) // warning: getPayloadAsText actually takes out the data from the buffer
                .flatMap(m -> sessionService.getAuthentication(session)) // get user session from websocket http headers
                .switchIfEmpty(Mono.error(new NoPermissionException()))
                .flatMap(auth -> {
                    if (!auth.isAuthenticated() || !(auth.getPrincipal() instanceof User))
                        return Mono.error(new NoPermissionException());

                    return Mono.zip(Mono.just(auth), submissionService.findSubmissionById(submissionId.toString()));
                })
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> Mono.zip(Mono.just(t.getT1()), Mono.just(t.getT2()), problemService.findProblemById(t.getT2().getProblemId())))
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(t -> {
                    Submission s = t.getT2();

                    // check session permission for submission
                    if (!s.hasPermissionToView(t.getT1(), t.getT3()))
                        return Mono.error(new NoPermissionException());

                    sessionService.addSession("/live-submission/" + submissionId.toString(), session, inputStream);

                    List<Submission.SubmissionBatchCase> l = new ArrayList<>();
                    for (var nl : s.getBatchCases()) l.addAll(nl);

                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        inputStream.onNext(session.textMessage(mapper.writeValueAsString(l)));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return Mono.empty();
                })
                .doOnError(e -> inputStream.onNext(session.textMessage("[]")))
        ).doFinally(signalType -> sessionService.removeSession("/live-submission/" + submissionId, session.getId()));
    }
}
