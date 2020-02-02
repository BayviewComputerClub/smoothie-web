package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.models.Submission;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadTestDataStreamObserver implements StreamObserver<SmoothieRunner.UploadTestDataResponse> {

    private Logger logger = LoggerFactory.getLogger(UploadTestDataStreamObserver.class);

    club.bayview.smoothieweb.services.SmoothieRunner runner;
    Submission submission;
    SmoothieRunner.TestSolutionRequest req;

    public UploadTestDataStreamObserver(Submission submission, club.bayview.smoothieweb.services.SmoothieRunner runner, SmoothieRunner.TestSolutionRequest req) {
        this.submission = submission;
        this.runner = runner;
        this.req = req;
    }

    @Override
    public void onNext(SmoothieRunner.UploadTestDataResponse value) { // will only be called once
        // TODO better error handling
        if (!value.getError().equals("")) {
            logger.info("UPLOAD TEST DATA ERROR: " + value.getError());
        }
    }

    @Override
    public void onError(Throwable t) {
        // TODO oh
    }

    @Override
    public void onCompleted() {
        // go back to grading
        runner.grade(req, submission);
    }
}
