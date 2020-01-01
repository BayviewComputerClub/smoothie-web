package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.controllers.LiveSubmissionController;
import club.bayview.smoothieweb.models.Submission;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class GraderStreamObserver implements StreamObserver<SmoothieRunner.TestSolutionResponse> {

    private LiveSubmissionController liveSubmissionController;
    private SmoothieSubmissionService submissionService;
    private SmoothieUserService userService;
    private SmoothieProblemService problemService;

    private Submission submission;

    private Logger logger = LoggerFactory.getLogger(GraderStreamObserver.class);

    public GraderStreamObserver(LiveSubmissionController liveSubmissionController, SmoothieSubmissionService submissionService, SmoothieUserService userService, SmoothieProblemService problemService, Submission submission) {
        this.liveSubmissionController = liveSubmissionController;
        this.submissionService = submissionService;
        this.userService = userService;
        this.problemService = problemService;
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

        userService.findUserById(submission.getUserId()).flatMap(user -> {
            if (user == null) return Mono.empty();
            if (user.getSolved().contains(submission.getProblemId())) return Mono.empty();

            user.getSolved().add(submission.getProblemId());
            return userService.saveUser(user).then(problemService.findProblemById(submission.getProblemId())).flatMap(problem -> {
                problem.setUsersSolved(problem.getUsersSolved()+1);
                return problemService.saveProblem(problem);
            });
        }).subscribe();
    }
}