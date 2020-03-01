package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.controllers.LiveSubmissionController;
import club.bayview.smoothieweb.models.Submission;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GraderStreamObserver implements StreamObserver<SmoothieRunner.TestSolutionResponse> {

    WebSocketSessionService webSocketSessionService = SmoothieWebApplication.context.getBean(WebSocketSessionService.class);
    SmoothieSubmissionService submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);
    SmoothieQueuedSubmissionService queuedSubmissionService = SmoothieWebApplication.context.getBean(SmoothieQueuedSubmissionService.class);
    SubmissionVerdictService verdictService = SmoothieWebApplication.context.getBean(SubmissionVerdictService.class);

    private Submission submission;

    private Logger logger = LoggerFactory.getLogger(GraderStreamObserver.class);

    private club.bayview.smoothieweb.services.SmoothieRunner runner;
    private SmoothieRunner.TestSolutionRequest req; // initial sending request

    private boolean terminated = false;

    ObjectMapper ob = new ObjectMapper();

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
            List<Submission.SubmissionBatchCase> socketSend = new ArrayList<>();
            for (var cases : submission.getBatchCases()) {
                for (var c : cases) {
                    if (c.getBatchNumber() == value.getTestCaseResult().getBatchNumber() && c.getCaseNumber() == value.getTestCaseResult().getCaseNumber()) {
                        c.setError(value.getTestCaseResult().getResultInfo());
                        c.setMemUsage(value.getTestCaseResult().getMemUsage());
                        c.setTime(value.getTestCaseResult().getTime());
                        c.setResultCode(value.getTestCaseResult().getResult());
                        socketSend.add(c);
                    }
                }
            }
            // send to websocket
            try {
                webSocketSessionService.sendToClients("/live-submission/" + submission.getId(), ob.writeValueAsString(socketSend));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
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
        verdictService.applyVerdictToSubmission(submission).subscribe();

        // find next task to do
        queuedSubmissionService.checkRunnersTask();
    }
}
