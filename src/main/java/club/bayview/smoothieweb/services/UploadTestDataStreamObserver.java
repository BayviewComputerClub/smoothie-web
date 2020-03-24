package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.submissions.RunnerTaskContextProcessorService;
import club.bayview.smoothieweb.services.submissions.RunnerTaskProcessorEvent;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadTestDataStreamObserver implements StreamObserver<SmoothieRunner.UploadTestDataResponse> {

    RunnerTaskContextProcessorService taskProcessorService = SmoothieWebApplication.context.getBean(RunnerTaskContextProcessorService.class);
    private Logger logger = LoggerFactory.getLogger(UploadTestDataStreamObserver.class);

    club.bayview.smoothieweb.services.SmoothieRunner runner;

    public UploadTestDataStreamObserver(club.bayview.smoothieweb.services.SmoothieRunner runner) {
        this.runner = runner;
    }

    @Override
    public void onNext(SmoothieRunner.UploadTestDataResponse value) { // will only be called once
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_UPLOAD_RECV_MSG)
                .uploadTestDataResponse(value)
                .build());
    }

    @Override
    public void onError(Throwable t) {
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_UPLOAD_ERR)
                .error(t)
                .build());
    }

    @Override
    public void onCompleted() {
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_UPLOAD_COMPLETE)
                .build());
    }
}
