package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.submissions.RunnerTaskContextProcessorService;
import club.bayview.smoothieweb.services.submissions.RunnerTaskProcessorEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GraderStreamObserver implements StreamObserver<SmoothieRunner.TestSolutionResponse> {

    RunnerTaskContextProcessorService taskProcessorService = SmoothieWebApplication.context.getBean(RunnerTaskContextProcessorService.class);
    private club.bayview.smoothieweb.services.SmoothieRunner runner;

    public GraderStreamObserver(club.bayview.smoothieweb.services.SmoothieRunner runner) {
        this.runner = runner;
    }

    @Override
    public void onNext(club.bayview.smoothieweb.SmoothieRunner.TestSolutionResponse value) {
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_GRADER_RECV_MSG)
                .testSolutionResponse(value)
                .build()
        );
    }

    @Override
    public void onError(Throwable t) {
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_GRADER_ERR)
                .error(t)
                .build());
    }

    @Override
    public void onCompleted() {
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_GRADER_COMPLETE)
                .build());
    }
}
