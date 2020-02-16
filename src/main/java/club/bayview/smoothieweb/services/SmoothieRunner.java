package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunnerAPIGrpc;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Runner;
import club.bayview.smoothieweb.models.Submission;
import com.google.protobuf.ByteString;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

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
        channel.notifyWhenStateChanged(ConnectivityState.READY, () -> {
            logger.info(String.format("Runner %s changed state: READY", name));
            SmoothieWebApplication.context.getBean(SmoothieQueuedSubmissionService.class).checkRunnersTask(); // TODO
        });
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

        var observer = getAsyncStub().testSolution(new GraderStreamObserver(submission, this, req));

        // send request
        observer.onNext(req);
        observer.onCompleted();
    }

    public void uploadTestData(club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest req, Submission submission) {
        logger.info(String.format("Uploading test data to runner %s for problem %s.", getName(), submission.getProblemId()));

        var observer = asyncStub.uploadProblemTestData(new UploadTestDataStreamObserver(submission, this, req));
        var problemBean = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);

        AtomicReference<String> hash = new AtomicReference<>(), testDataId = new AtomicReference<>();

        problemBean.findProblemById(submission.getProblemId())
                .flatMap(p -> { // get problem
                    testDataId.set(p.getTestDataId());
                    return p.getTestDataHash();
                })
                .flatMapMany(h -> { // get test data hash
                    hash.set(h);
                    try {
                        return problemBean.findRawProblemTestDataFlux(testDataId.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Flux.error(e);
                    }
                })
                // send each data chunk
                .doOnNext(bytes -> observer.onNext(club.bayview.smoothieweb.SmoothieRunner.UploadTestDataRequest.newBuilder()
                        .setDataChunk(ByteString.copyFrom(bytes))
                        .setProblemId(submission.getProblemId())
                        .setTestDataHash(hash.get())
                        .setFinishedUploading(false)
                        .build()))
                // data chunk sending complete
                .doOnComplete(() -> observer.onNext(club.bayview.smoothieweb.SmoothieRunner.UploadTestDataRequest.newBuilder()
                        .setDataChunk(ByteString.copyFrom(new byte[0]))
                        .setProblemId(submission.getProblemId())
                        .setTestDataHash(hash.get())
                        .setFinishedUploading(true)
                        .build()))
                .subscribe();
    }

    @Override
    public int compareTo(SmoothieRunner runner) {
        var s1 = this.getHealth();
        var s2 = runner.getHealth();
        return Long.compare(s1.getNumOfTasksInQueue(), s2.getNumOfTasksToBeDone());
    }
}
