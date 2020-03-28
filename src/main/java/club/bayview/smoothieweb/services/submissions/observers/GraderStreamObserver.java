package club.bayview.smoothieweb.services.submissions.observers;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.submissions.RunnerTaskContextProcessorService;
import club.bayview.smoothieweb.services.submissions.RunnerTaskProcessorEvent;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.Setter;

public class GraderStreamObserver implements StreamObserver<SmoothieRunner.TestSolutionResponse> {

    RunnerTaskContextProcessorService taskProcessorService = SmoothieWebApplication.context.getBean(RunnerTaskContextProcessorService.class);
    private club.bayview.smoothieweb.services.submissions.SmoothieRunner runner;

    @Getter
    @Setter
    boolean terminated = false; // set to true once this grader stream observer is abandoned by RunnerTaskContextProcessor, but still running

    public GraderStreamObserver(club.bayview.smoothieweb.services.submissions.SmoothieRunner runner) {
        this.runner = runner;
    }

    @Override
    public void onNext(club.bayview.smoothieweb.SmoothieRunner.TestSolutionResponse value) {
        if (isTerminated()) return;
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_GRADER_RECV_MSG)
                .testSolutionResponse(value)
                .build()
        );
    }

    @Override
    public void onError(Throwable t) {
        if (isTerminated()) return;
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_GRADER_ERR)
                .error(t)
                .build());
    }

    @Override
    public void onCompleted() {
        if (isTerminated()) return;
        taskProcessorService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                .eventType(RunnerTaskProcessorEvent.EventType.RUNNER_GRADER_COMPLETE)
                .build());
    }
}
