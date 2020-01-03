package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.controllers.LiveSubmissionController;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.util.Verdict;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

public class GraderStreamObserver implements StreamObserver<SmoothieRunner.TestSolutionResponse> {

    LiveSubmissionController liveSubmissionController;
    SmoothieSubmissionService submissionService;
    SmoothieUserService userService;
    SmoothieProblemService problemService;

    private Submission submission;

    private Logger logger = LoggerFactory.getLogger(GraderStreamObserver.class);

    public GraderStreamObserver(Submission submission) {
        this.liveSubmissionController = SmoothieWebApplication.context.getBean(LiveSubmissionController.class);
        this.submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);
        this.userService = SmoothieWebApplication.context.getBean(SmoothieUserService.class);
        this.problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);
        this.submission = submission;
    }

    @Override
    public void onNext(club.bayview.smoothieweb.SmoothieRunner.TestSolutionResponse value) {
        if (!value.getCompileError().equals("")) { // compile error
            submission.setCompileError(value.getCompileError());
        } else if (value.getCompletedTesting()) { // testing has completed
            submission.setJudgingCompleted(true);
        } else {
            for (var cases : submission.getBatchCases()) {
                for (var c : cases) {
                    if (c.getBatchNumber() == value.getTestCaseResult().getBatchNumber() && c.getCaseNumber() == value.getTestCaseResult().getCaseNumber()) {
                        c.setError(value.getTestCaseResult().getResultInfo());
                        c.setMemUsage(value.getTestCaseResult().getMemUsage());
                        c.setTime(value.getTestCaseResult().getTime());
                        c.setResultCode(value.getTestCaseResult().getResult());

                        // send to websocket
                        liveSubmissionController.sendSubmissionBatch(submission.getId(), c);
                    }
                }
            }
        }

        submissionService.saveSubmission(submission).subscribe();
    }


    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
        logger.error(t.getMessage());
    }

    @Override
    public void onCompleted() {
        logger.info("Judging has completed for submission " + submission.getId() + ".");

        // get verdict
        submission.determineVerdict();
        submissionService.saveSubmission(submission).subscribe();

        // do stuff with verdict
        if (submission.getVerdict().equals(Verdict.AC.toString())) {
            userService.findUserById(submission.getUserId()).flatMap(user -> {
                if (user == null) return Mono.empty();
                if (user.getSolved().contains(submission.getProblemId())) return Mono.empty();

                user.getSolved().add(submission.getProblemId()); // first time solving
                return userService.saveUser(user).then(problemService.findProblemById(submission.getProblemId())).flatMap(problem -> {

                    problem.setUsersSolved(problem.getUsersSolved() + 1);
                    user.setPoints(user.getPoints()+problem.getTotalPointsWorth()); // TODO partial

                    return Mono.zip(userService.saveUser(user), problemService.saveProblem(problem));
                });
            }).subscribe();
        }
    }
}
