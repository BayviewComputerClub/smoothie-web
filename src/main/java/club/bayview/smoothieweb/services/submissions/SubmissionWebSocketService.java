package club.bayview.smoothieweb.services.submissions;

import club.bayview.smoothieweb.controllers.websocket.LiveSubmissionController;
import club.bayview.smoothieweb.controllers.websocket.LiveSubmissionListController;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.WebSocketSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Service
public class SubmissionWebSocketService {

    @Autowired
    WebSocketSessionService webSocketSessionService;

    @Autowired
    SmoothieProblemService problemService;

    ObjectMapper om = new ObjectMapper();

    public void sendLiveSubmission(String route, LiveSubmissionController.LiveSubmissionData data) {
        try {
            webSocketSessionService.sendToClients(route, om.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Mono<Void> sendLiveSubmissionListEntry(Submission s) {
        return Mono.zip(problemService.findProblemById(s.getProblemId()), LiveSubmissionListController.LiveSubmissionListWSResponse.fromSubmission(s))
                .doOnNext(t -> {
                    var res = t.getT2();
                    var p = t.getT1();
                    sendLiveSubmissionListEntryHelper("/live-submission-u/" + s.getUserId(), p, s, res);
                    sendLiveSubmissionListEntryHelper("/live-submission-p/" + s.getProblemId(), p, s, res);
                    sendLiveSubmissionListEntryHelper("/live-submission-up/" + s.getUserId() + "/" + s.getProblemId(), p, s, res);
                    if (res.getContestName() != null) {
                        sendLiveSubmissionListEntryHelper("/live-submission-c/" + s.getContestId(), p, s, res);
                        sendLiveSubmissionListEntryHelper("/live-submission-uc/" + s.getUserId() + "/" + s.getContestId(), p, s, res);
                        sendLiveSubmissionListEntryHelper("/live-submission-pc/" + s.getProblemId() + "/" + s.getContestId(), p, s, res);
                        sendLiveSubmissionListEntryHelper("/live-submission-upc/" + s.getUserId() + "/" + s.getProblemId() + "/" + s.getContestId(), p, s, res);
                    }
                }).then();
    }

    // manually send web socket message, since the permissionToView field needs to be filled
    private void sendLiveSubmissionListEntryHelper(String route, Problem problem, Submission submission, LiveSubmissionListController.LiveSubmissionListWSResponse res) {
        var sessions = webSocketSessionService.getSessions();
        if (sessions.containsKey(route)) {
            DefaultDataBufferFactory f = new DefaultDataBufferFactory();

            sessions.get(route).forEach((id, session) -> {
                res.setPermissionToView(submission.hasPermissionToView(webSocketSessionService.getSessionAuths().get(id), problem));
                try {
                    session.onNext(new WebSocketMessage(WebSocketMessage.Type.TEXT, f.wrap(om.writeValueAsString(Arrays.asList(res)).getBytes())));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });

        }
    }

}
