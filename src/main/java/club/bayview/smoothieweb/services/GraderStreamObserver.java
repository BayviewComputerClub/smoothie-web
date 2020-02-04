package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.controllers.LiveSubmissionController;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.Verdict;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class GraderStreamObserver implements StreamObserver<SmoothieRunner.TestSolutionResponse> {

    LiveSubmissionController liveSubmissionController = SmoothieWebApplication.context.getBean(LiveSubmissionController.class);
    SmoothieSubmissionService submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);
    SmoothieUserService userService = SmoothieWebApplication.context.getBean(SmoothieUserService.class);
    SmoothieProblemService problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);
    SmoothieQueuedSubmissionService queuedSubmissionService = SmoothieWebApplication.context.getBean(SmoothieQueuedSubmissionService.class);

    private Submission submission;

    private Logger logger = LoggerFactory.getLogger(GraderStreamObserver.class);

    private club.bayview.smoothieweb.services.SmoothieRunner runner;
    private SmoothieRunner.TestSolutionRequest req; // initial sending request

    private boolean terminated = false;

    public GraderStreamObserver(Submission submission, club.bayview.smoothieweb.services.SmoothieRunner runner, SmoothieRunner.TestSolutionRequest req) {
        this.submission = submission;
        this.runner = runner;
        this.req = req;
    }

    @Override
    public void onNext(club.bayview.smoothieweb.SmoothieRunner.TestSolutionResponse value) {
        // check if test data needs to be uploaded first
        if (value.getTestDataNeedUpload()) {
            terminated = true; // prevent onCompleted from going through
            runner.uploadTestData(req, submission); // upload test data, and then grade again
            return;
        }

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
        if (terminated) return;

        logger.info("Judging has completed for submission " + submission.getId() + ".");

        // store verdict and update points if necessary
        Mono.zip(userService.findUserById(submission.getUserId()), problemService.findProblemById(submission.getProblemId()))
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    Problem problem = tuple.getT2();

                    submission.determineVerdict();
                    submission.determinePoints(problem);

                    // if first time solve
                    if (submission.getVerdict().equals(Verdict.AC.toString())) {
                        if (!user.getSolved().contains(problem.getId())) {
                            user.getSolved().add(problem.getId());
                            problem.setUsersSolved(problem.getUsersSolved() + 1);
                        }
                    }

                    // update points if the submission is higher
                    if (!user.getProblemsAttempted().containsKey(problem.getId()) || user.getProblemsAttempted().get(problem.getId()) < submission.getPoints()) {
                        if (user.getProblemsAttempted().containsKey(problem.getId()) && user.getProblemsAttempted().get(problem.getId()) < submission.getPoints()) {
                            user.setPoints(user.getPoints() - user.getProblemsAttempted().get(problem.getId()));
                        }

                        user.getProblemsAttempted().put(problem.getId(), submission.getPoints());
                        user.setPoints(user.getPoints() + submission.getPoints());
                    }

                    return Mono.zip(userService.saveUser(user), problemService.saveProblem(problem), submissionService.saveSubmission(submission));
                })
                .subscribe();

        // find next task to do
        queuedSubmissionService.checkRunnersTask();
    }
}
