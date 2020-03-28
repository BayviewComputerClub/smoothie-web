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
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.core.publisher.UnicastProcessor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicReference;

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
        boolean permissionToView; // needs to be set manually

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
        SmoothieProblemService problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);

        UnicastProcessor<WebSocketMessage> inputStream = UnicastProcessor.create();
        LiveSubmissionListWSRequest req = new LiveSubmissionListWSRequest();
        StringBuilder route = new StringBuilder();
        AtomicReference<Authentication> auth = new AtomicReference<>();

        HashMap<String, Submission> submissions = new HashMap<>();
        HashSet<String> problemIds = new HashSet<>();

        return sessionService.setupInput(session, inputStream, Flux.zip(sessionService.takeOneMsg(session), sessionService.getAuthentication(session))
                .flatMap(t -> {
                    try {
                        om.readerForUpdating(req).readValue(t.getT1().getPayloadAsText());
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    auth.set(t.getT2());
                    return req.fill();
                })

                // get submissions from request
                .flatMap(u -> getCorrespondingSubmissions(req, route)) // must be flatmap instead of then for some reason idk

                // build cache for submissions to problems
                .doOnNext(s -> submissions.put(s.getId(), s))
                .doOnNext(s -> problemIds.add(s.getProblemId()))

                // convert submissions to responses
                .flatMap(LiveSubmissionListWSResponse::fromSubmission)
                .collectList()
                .flatMap(wsl -> Mono.zip(Mono.just(wsl), problemService.getProblemIdToProblemMap(Flux.fromIterable(problemIds))))

                // send initial list to client
                .doOnNext(t -> {
                    // add field on whether submission can be viewed
                    t.getT1().forEach(sl -> {
                        Submission s = submissions.get(sl.getSubmissionId());
                        sl.setPermissionToView(s.hasPermissionToView(auth.get(), t.getT2().get(s.getProblemId())));
                    });

                    // send initial submissions
                    try {
                        inputStream.onNext(session.textMessage(om.writeValueAsString(t.getT1())));
                    } catch (JsonProcessingException e) {
                        logger.error("Error parsing json to send for live submission list: ", e);
                    }

                    // add to websocket sessions
                    sessionService.addSession(route.toString(), session, inputStream, auth.get());
                })
                .doOnError(e -> {
                    e.printStackTrace();
                    inputStream.onNext(session.textMessage("[]"));
                })
        ).doFinally(st -> sessionService.removeSession(route.toString(), session.getId()));
    }

}
