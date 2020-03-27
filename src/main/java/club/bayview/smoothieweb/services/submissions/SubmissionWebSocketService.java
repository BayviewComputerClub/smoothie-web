package club.bayview.smoothieweb.services.submissions;

import club.bayview.smoothieweb.controllers.websocket.LiveSubmissionController;
import club.bayview.smoothieweb.controllers.websocket.LiveSubmissionListController;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.WebSocketSessionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Service
public class SubmissionWebSocketService {

    @Autowired
    WebSocketSessionService webSocketSessionService;

    ObjectMapper om = new ObjectMapper();

    public void sendLiveSubmission(String route, LiveSubmissionController.LiveSubmissionData data) {
        try {
            webSocketSessionService.sendToClients(route, om.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public Mono<Void> sendLiveSubmissionListEntry(Submission s) {
        return LiveSubmissionListController.LiveSubmissionListWSResponse.fromSubmission(s)
                .map(Arrays::asList)
                .doOnNext(res -> {
                    try {
                        String json = om.writeValueAsString(res);
                        webSocketSessionService.sendToClients("/live-submission-u/" + s.getUserId(), json);
                        webSocketSessionService.sendToClients("/live-submission-p/" + s.getProblemId(), json);
                        webSocketSessionService.sendToClients("/live-submission-up/" + s.getUserId() + "/" + s.getProblemId(), json);
                        if (res.get(0).getContestName() != null) {
                            webSocketSessionService.sendToClients("/live-submission-c/" + s.getContestId(), json);
                            webSocketSessionService.sendToClients("/live-submission-uc/" + s.getUserId() + "/" + s.getContestId(), json);
                            webSocketSessionService.sendToClients("/live-submission-pc/" + s.getProblemId() + "/" + s.getContestId(), json);
                            webSocketSessionService.sendToClients("/live-submission-upc/" + s.getUserId() + "/" + s.getProblemId() + "/" + s.getContestId(), json);
                        }
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }).then();
    }

}
