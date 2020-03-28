package club.bayview.smoothieweb.controllers.websocket;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Contest;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.*;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.PageUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.publisher.UnicastProcessor;

/**
 * Websocket controller:
 * /ws/live-submission-list
 * <p>
 * Must send a {@link LiveSubmissionListWSRequest} to the route first.
 * Sends a stream of messages, where each message is an array of {@link LiveSubmissionListWSResponse} in JSON.
 * </p>
 */

public class LiveSubmissionListController implements WebSocketHandler {
    static final int PAGE_SIZE = Integer.parseInt(PageUtil.DEFAULT_PAGE_SIZE);

    // TODO check permissions for contest viewing

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Getter
    @Setter
    @ToString
    public static class LiveSubmissionListWSRequest {
        @JsonProperty("username")
        String username;
        @JsonProperty("problemId")
        String problemName;
        @JsonProperty("contestId")
        String contestName;
        @JsonIgnore
        User user = null;
        @JsonIgnore
        Problem problem = null;
        @JsonIgnore
        Contest contest = null;

        // fill fields with given info
        public Mono<Signal<Void>> fill() {
            return Mono.when(getUsername() == null ? Mono.just("") : SmoothieWebApplication.context.getBean(SmoothieUserService.class)
                            .findUserByHandle(getUsername())
                            .doOnNext(this::setUser).then(),
                    getProblemName() == null ? Mono.just("") : SmoothieWebApplication.context.getBean(SmoothieProblemService.class)
                            .findProblemByName(getProblemName())
                            .doOnNext(this::setProblem)
                            .then(),
                    getContestName() == null ? Mono.just("") : SmoothieWebApplication.context.getBean(SmoothieContestService.class)
                            .findContestByName(getContestName())
                            .doOnNext(this::setContest)
                            .then()
            ).materialize();
        }
    }

    @Getter
    @Setter
    public static class LiveSubmissionListWSResponse {
        String submissionId, language, userName, problemName, contestName, problemPrettyName, contestPrettyName;
        Submission.SubmissionStatus submissionStatus;
        String verdict;
        long time, pointsAwarded, pointsMax;

        public static Mono<LiveSubmissionListWSResponse> fromSubmission(Submission s) {
            LiveSubmissionListWSResponse res = new LiveSubmissionListWSResponse();
            res.setSubmissionId(s.getId());
            res.setLanguage(s.getLang());
            res.setSubmissionStatus(s.getStatus());
            res.setVerdict(s.getVerdict());
            res.setTime(s.getTimeSubmitted());
            res.setPointsAwarded(s.getPoints());
            res.setPointsMax(s.getMaxPoints());

            // resolve ids
            return Mono.when(s.getUserId() == null ? Mono.just("") : SmoothieWebApplication.context.getBean(SmoothieUserService.class)
                            .findUserById(s.getUserId())
                            .doOnNext(u -> res.setUserName(u.getHandle())),
                    s.getProblemId() == null ? Mono.just("") : SmoothieWebApplication.context.getBean(SmoothieProblemService.class)
                            .findProblemById(s.getProblemId())
                            .doOnNext(p -> res.setProblemName(p.getName()))
                            .doOnNext(p -> res.setProblemPrettyName(p.getPrettyName())),
                    s.getContestId() == null ? Mono.just("") : SmoothieWebApplication.context.getBean(SmoothieContestService.class)
                            .findContestById(s.getContestId())
                            .doOnNext(c -> res.setContestName(c.getName()))
                            .doOnNext(c -> res.setContestPrettyName(c.getPrettyName()))
            ).then(Mono.just(res));
        }
    }

    ObjectMapper om = new ObjectMapper();

    private Flux<Submission> getCorrespondingSubmissions(LiveSubmissionListWSRequest req, StringBuilder route) {
        SmoothieSubmissionService submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);
        if (req.getUser() != null && req.getProblem() == null && req.getContest() == null) {
            route.append("/live-submission-u/" + req.getUser().getId());
            return submissionService.findSubmissionsByUser(req.getUser().getId(), PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "timeSubmitted"));
        } else if (req.getUser() == null && req.getProblem() != null && req.getContest() == null) {
            route.append("/live-submission-p/" + req.getProblem().getId());
            return submissionService.findSubmissionsByProblem(req.getProblem().getId(), PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "timeSubmitted"));
        } else if (req.getUser() == null && req.getProblem() == null && req.getContest() != null) {
            route.append("/live-submission-c/" + req.getContest().getId());
            return submissionService.findSubmissionsForContest(req.getContest().getId(), PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "timeSubmitted"));
        } else if (req.getUser() != null && req.getProblem() != null && req.getContest() == null) {
            route.append("/live-submission-up/" + req.getUser().getId() + "/" + req.getProblem().getId());
            return submissionService.findSubmissionsByUserForProblem(req.getUser().getId(), req.getProblem().getId(), PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "timeSubmitted"));
        } else if (req.getUser() == null && req.getProblem() != null && req.getContest() != null) {
            route.append("/live-submission-pc/" + req.getProblem().getId() + "/" + req.getContest().getId());
            return submissionService.findSubmissionsForContestAndProblem(req.getContest().getId(), req.getProblem().getId(), PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "timeSubmitted"));
        } else if (req.getUser() != null && req.getProblem() == null && req.getContest() != null) {
            route.append("/live-submission-uc/" + req.getUser().getId() + "/" + req.getContest().getId());
            return submissionService.findSubmissionsByUserForContest(req.getUser().getId(), req.getContest().getId(), PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "timeSubmitted"));
        } else if (req.getUser() != null && req.getProblem() != null && req.getContest() != null) {
            route.append("/live-submission-upc/" + req.getUser().getId() + "/" + req.getProblem().getId() + "/" + req.getContest().getId());
            return submissionService.findSubmissionsByUserForProblemAndContest(req.getUser().getId(), req.getProblem().getId(), req.getContest().getId(), PageRequest.of(0, PAGE_SIZE, Sort.Direction.DESC, "timeSubmitted"));
        }
        return Flux.error(new NotFoundException());
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        WebSocketSessionService sessionService = SmoothieWebApplication.context.getBean(WebSocketSessionService.class);

        UnicastProcessor<WebSocketMessage> inputStream = UnicastProcessor.create();
        LiveSubmissionListWSRequest req = new LiveSubmissionListWSRequest();
        StringBuilder route = new StringBuilder();

        return sessionService.setupInput(session, inputStream, sessionService.takeOneMsg(session)
                .flatMap(p -> {
                    try {
                        om.readerForUpdating(req).readValue(p.getPayloadAsText());
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    return req.fill();
                })
                .flatMap(u -> getCorrespondingSubmissions(req, route)) // must be flatmap instead of then for some reason idk
                .flatMap(LiveSubmissionListWSResponse::fromSubmission)
                .collectList()
                .doOnNext(wsl -> { // send initial list to client
                    // add to websocket sessions
                    sessionService.addSession(route.toString(), session, inputStream);

                    // send initial submissions
                    try {
                        inputStream.onNext(session.textMessage(om.writeValueAsString(wsl)));
                    } catch (JsonProcessingException e) {
                        logger.error("Error parsing json to send for live submission list: ", e);
                    }
                })
                .doOnError(e -> {
                    e.printStackTrace();
                    inputStream.onNext(session.textMessage("[]"));
                })
        ).doFinally(st -> sessionService.removeSession(route.toString(), session.getId()));
    }

}
