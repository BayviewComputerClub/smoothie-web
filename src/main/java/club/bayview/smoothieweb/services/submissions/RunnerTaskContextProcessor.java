package club.bayview.smoothieweb.services.submissions;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.controllers.websocket.LiveSubmissionController;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.QueuedSubmission;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * A worker instance that processes runner tasks for a smoothie runner instance.
 */

public class RunnerTaskContextProcessor implements Runnable {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    // services
    SmoothieSubmissionService submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);
    SmoothieQueuedSubmissionService queuedSubmissionService = SmoothieWebApplication.context.getBean(SmoothieQueuedSubmissionService.class);
    SubmissionVerdictService verdictService = SmoothieWebApplication.context.getBean(SubmissionVerdictService.class);
    SmoothieProblemService problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);
    SubmissionWebSocketService submissionWebSocketService = SmoothieWebApplication.context.getBean(SubmissionWebSocketService.class);

    // reactive queue
    UnicastProcessor<RunnerTaskProcessorEvent> taskQueue = UnicastProcessor.create();

    String currentSubmissionId, currentQueuedSubmissionId;
    club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest currentTestSolutionRequest;
    SmoothieRunner runner;

    public RunnerTaskContextProcessor(SmoothieRunner runner) {
        this.runner = runner;
    }

    // assumes that judge_submission would not be sent if a submission is ongoing
    @Override
    public void run() {
        logger.info("Started worker thread for runner {}.", runner.getName());
        // todo error recover
        taskQueue.concatMap(this::processEvent).subscribe();
    }

    public Mono<Void> processEvent(RunnerTaskProcessorEvent ev) {
        switch (ev.eventType) {
            case RUNNER_GRADER_RECV_MSG:
                return graderReceivedMessage(ev.testSolutionResponse);
            case RUNNER_GRADER_COMPLETE:
                return graderCompletedMessage();
            case RUNNER_GRADER_ERR:
                return graderErrorMessage(ev.error);
            case RUNNER_UPLOAD_RECV_MSG:
                return uploadReceivedMessage(ev.uploadTestDataResponse);
            case RUNNER_UPLOAD_COMPLETE:
                return uploadCompletedMessage();
            case RUNNER_UPLOAD_ERR:
                return uploadErrorMessage(ev.error);
            case RUNNER_TRANSIENT_FAILURE:
                break; // TODO
            case CANCEL_SUBMISSION:
                return cancelSubmission();
            case JUDGE_SUBMISSION:
                return judgeSubmission(ev.getQueuedSubmission());
            case STOP:
                // TODO submission is not done but cleanstop is called
                return Mono.error(new Exception());
        }
        return Mono.empty();
    }

    public Mono<Void> judgeSubmission(QueuedSubmission qs) {
        runner.setOccupied(true);

        currentQueuedSubmissionId = qs.getId();
        currentSubmissionId = qs.getSubmissionId();

        return submissionService.findSubmissionById(currentSubmissionId)
                .doOnNext(s -> {
                    s.setRunnerId(runner.getId());
                    s.setStatus(Submission.SubmissionStatus.JUDGING);
                    // send to websocket
                    submissionWebSocketService.sendLiveSubmissionListEntry(s).subscribe();
                })
                .flatMap(s -> Mono.zip(submissionService.saveSubmission(s), problemService.findProblemById(s.getProblemId())))
                .flatMap(t -> Mono.zip(Mono.just(t.getT1()), toTestSolutionRequest(t.getT2(), t.getT1())))
                .doOnNext(t -> graderStreamObserver = runner.grade(t.getT2(), t.getT1()))
                .doOnNext(t -> currentTestSolutionRequest = t.getT2())
                .then();
    }

    public Mono<Void> cancelSubmission() {
        runner.setOccupied(false);
        graderStreamObserver.onError(new Exception());
        return submissionService.findSubmissionById(currentSubmissionId)
                .flatMap(s -> {
                    s.setStatus(Submission.SubmissionStatus.CANCELLED);
                    s.setJudgingCompleted(true);
                    return Mono.zip(submissionService.saveSubmission(s), submissionWebSocketService.sendLiveSubmissionListEntry(s));
                }).then();
    }

    public Mono<Void> uploadErrorMessage(Throwable t) {
        t.printStackTrace();
        logger.error(t.getMessage());
        return cancelSubmission();
    }

    public Mono<Void> uploadReceivedMessage(club.bayview.smoothieweb.SmoothieRunner.UploadTestDataResponse res) {
        if (!res.getError().equals("")) {
            logger.error("Test Data Upload Error: " + res.getError());
            return cancelSubmission();
        }
        return Mono.empty();
    }

    public Mono<Void> uploadCompletedMessage() {
        // go back to grading
        return submissionService.findSubmissionById(currentSubmissionId)
                .doOnNext(s -> graderStreamObserver = runner.grade(currentTestSolutionRequest, s))
                .then();
    }

    boolean graderTerminatedEarly = false;
    StreamObserver<club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest> graderStreamObserver;

    public Mono<Void> graderErrorMessage(Throwable t) {
        t.printStackTrace();
        logger.error(t.getMessage());
        return cancelSubmission();
    }

    public Mono<Void> graderCompletedMessage() {
        if (graderTerminatedEarly) return Mono.empty();

        runner.setOccupied(false);

        return submissionService.findSubmissionById(currentSubmissionId)
                .flatMap(s -> {
                    logger.info("Judging has completed for submission " + s.getId() + ".");

                    // store verdict and update points if necessary
                    s.setStatus(Submission.SubmissionStatus.COMPLETE);

                    // todo find next task to do
                    queuedSubmissionService.checkRunnersTask();
                    return verdictService.applyVerdictToSubmission(s)
                            // send to websocket
                            .then(submissionWebSocketService.sendLiveSubmissionListEntry(s));
                });
    }

    public Mono<Void> graderReceivedMessage(club.bayview.smoothieweb.SmoothieRunner.TestSolutionResponse res) {
        return submissionService.findSubmissionById(currentSubmissionId)
                .flatMap(s -> {
                    // check if test data needs to be uploaded first
                    if (res.getTestDataNeedUpload()) {
                        graderTerminatedEarly = true; // prevent onCompleted from going through
                        runner.uploadTestData(s); // upload test data, and then grade again
                        return Mono.empty();
                    }

                    if (!res.getCompileError().equals("")) { // compile error
                        s.setCompileError(res.getCompileError());
                        s.setStatus(Submission.SubmissionStatus.COMPLETE);

                        // send to websocket
                        submissionWebSocketService.sendLiveSubmission("/live-submission/" + s.getId(), LiveSubmissionController.LiveSubmissionData.builder().compileError(s.getCompileError()).status(s.getStatus()).build());
                    } else if (res.getCompletedTesting()) { // testing has completed
                        s.setJudgingCompleted(true);
                        s.setStatus(Submission.SubmissionStatus.COMPLETE);

                        // send to websocket
                        submissionWebSocketService.sendLiveSubmission("/live-submission/" + s.getId(), LiveSubmissionController.LiveSubmissionData.builder().status(s.getStatus()).build());
                    } else { // result for test case
                        // update submission
                        List<Submission.SubmissionBatchCase> socketSend = new ArrayList<>();
                        for (var cases : s.getBatchCases()) {
                            for (var c : cases.getCases()) {
                                if (c.getBatchNumber() == res.getTestCaseResult().getBatchNumber() && c.getCaseNumber() == res.getTestCaseResult().getCaseNumber()) {
                                    c.setError(res.getTestCaseResult().getResultInfo());
                                    c.setMemUsage(res.getTestCaseResult().getMemUsage());
                                    c.setTime(res.getTestCaseResult().getTime());
                                    c.setResultCode(res.getTestCaseResult().getResult());
                                    socketSend.add(c);
                                }
                            }
                        }
                        // send to websocket
                        submissionWebSocketService.sendLiveSubmission("/live-submission/" + s.getId(), LiveSubmissionController.LiveSubmissionData.builder().batchCases(socketSend).status(s.getStatus()).build());
                    }
                    return submissionService.saveSubmission(s);
                }).then();
    }

    // create grpc object to send
    private Mono<club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest> toTestSolutionRequest(Problem p, Submission s) {
        return p.getGRPCObject(s.getLang())
                .flatMap(grpcProblem -> Mono.just(club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest.newBuilder()
                        .setTestBatchEvenIfFailed(false)
                        .setCancelTesting(false)
                        .setProblem(grpcProblem)
                        .setSolution(club.bayview.smoothieweb.SmoothieRunner.Solution.newBuilder()
                                .setCode(s.getCode() == null ? "": s.getCode())
                                .setLanguage(s.getLang() == null ? "" : s.getLang())
                                .build())
                        .build()));
    }
}
