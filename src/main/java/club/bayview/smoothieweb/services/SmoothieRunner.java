package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunnerAPIGrpc;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Runner;
import club.bayview.smoothieweb.models.Submission;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmoothieRunner implements Comparable<SmoothieRunner> {

    private ManagedChannel channel;

    @Getter
    private String id, name;

    private SmoothieRunnerAPIGrpc.SmoothieRunnerAPIBlockingStub blockingStub;
    private SmoothieRunnerAPIGrpc.SmoothieRunnerAPIStub asyncStub;
    private SmoothieRunnerAPIGrpc.SmoothieRunnerAPIFutureStub futureStub;

    private Logger logger = LoggerFactory.getLogger(SmoothieRunner.class);

    public SmoothieRunner(Runner runner) {
        this(runner.getId(), runner.getName(), runner.getHost(), runner.getPort());
    }

    public SmoothieRunner(String id, String name, String host, int port) {
        this(id, name, ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    public SmoothieRunner(String id, String name, ManagedChannelBuilder<?> channelBuilder) {
        this.id = id;
        this.name = name;
        channel = channelBuilder.build();

        blockingStub = SmoothieRunnerAPIGrpc.newBlockingStub(channel);
        asyncStub = SmoothieRunnerAPIGrpc.newStub(channel);
        futureStub = SmoothieRunnerAPIGrpc.newFutureStub(channel);

        // notifiers
        channel.notifyWhenStateChanged(ConnectivityState.READY, () -> logger.info(String.format("Runner %s changed state: READY", name)));
        channel.notifyWhenStateChanged(ConnectivityState.CONNECTING, () -> logger.info(String.format("Runner %s changed state: CONNECTING", name)));
        channel.notifyWhenStateChanged(ConnectivityState.IDLE, () -> logger.info(String.format("Runner %s changed state: IDLE", name)));
        channel.notifyWhenStateChanged(ConnectivityState.TRANSIENT_FAILURE, () -> logger.info(String.format("Runner %s changed state: TRANSIENT_FAILURE", name)));
        channel.notifyWhenStateChanged(ConnectivityState.SHUTDOWN, () -> logger.info(String.format("Runner %s changed state: SHUTDOWN", name)));
    }

    public SmoothieRunnerAPIGrpc.SmoothieRunnerAPIBlockingStub getBlockingStub() {
        return blockingStub;
    }

    public SmoothieRunnerAPIGrpc.SmoothieRunnerAPIStub getAsyncStub() {
        return asyncStub;
    }

    public SmoothieRunnerAPIGrpc.SmoothieRunnerAPIFutureStub getFutureStub() {
        return futureStub;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public ConnectivityState getStatus() {
        return channel.getState(true);
    }

    public club.bayview.smoothieweb.SmoothieRunner.ServiceHealth getHealth() {
        return getBlockingStub().health(club.bayview.smoothieweb.SmoothieRunner.Empty.getDefaultInstance());
    }

    public void cleanStop() {
        // clean shutdown, allowing threads to finish
        channel.shutdown();
    }

    /**
     * Run a full grader session asynchronously
     */

    public void grade(club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest req, Submission submission) {
        logger.info(String.format("Runner %s grading submission %s for problem %s.", getName(), submission.getId(), submission.getProblemId()));

        submission.setRunnerId(getId());
        SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class).saveSubmission(submission).subscribe();

        StreamObserver<club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest> observer = getAsyncStub().testSolution(new GraderStreamObserver(submission));

        // send request
        observer.onNext(req);
        observer.onCompleted();
    }

    @Override
    public int compareTo(SmoothieRunner runner) {
        var s1 = this.getHealth();
        var s2 = runner.getHealth();
        return Long.compare(s1.getNumOfTasksInQueue(), s2.getNumOfTasksToBeDone());
    }
}
