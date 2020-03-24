package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunnerAPIGrpc;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Runner;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.submissions.RunnerTaskContextProcessorService;
import club.bayview.smoothieweb.services.submissions.RunnerTaskProcessorEvent;
import com.google.protobuf.ByteString;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

public class SmoothieRunner implements Comparable<SmoothieRunner> {

    private ManagedChannel channel;

    @Getter
    private String id, name;

    @Getter
    @Setter
    private boolean occupied = false; // whether or not the runner is occupied with a task (from runnertaskcontextprocessor)

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
        this.channel = channelBuilder.build();

        blockingStub = SmoothieRunnerAPIGrpc.newBlockingStub(channel);
        asyncStub = SmoothieRunnerAPIGrpc.newStub(channel);
        futureStub = SmoothieRunnerAPIGrpc.newFutureStub(channel);

        // notifiers
        channel.notifyWhenStateChanged(ConnectivityState.READY, () -> {
            logger.info(String.format("Runner %s changed state: READY", name));
            try {
                SmoothieWebApplication.context.getBean(SmoothieQueuedSubmissionService.class).checkRunnersTask(); // TODO
            } catch (NullPointerException ignored) {
            } // if the bean has not initialized yet
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

    // clean shutdown, allowing threads to finish
    public void cleanStop() {
        channel.shutdown();
        SmoothieWebApplication.context.getBean(RunnerTaskContextProcessorService.class)
                .addTask(getId(), RunnerTaskProcessorEvent.builder().eventType(RunnerTaskProcessorEvent.EventType.STOP).build());
    }

    // Run a full grader session asynchronously
    public StreamObserver<club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest> grade(club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest req, Submission submission) {
        logger.info(String.format("Runner %s grading submission %s for problem %s.", getName(), submission.getId(), submission.getProblemId()));

        var observer = getAsyncStub().testSolution(new GraderStreamObserver(this));
        // send request
        observer.onNext(req);
        // mark end of requests TODO allow cancellation of submissions
        observer.onCompleted();
        return observer;
    }

    public void uploadTestData(Submission submission) {
        logger.info(String.format("Uploading test data to runner %s for problem %s.", getName(), submission.getProblemId()));

        var observer = asyncStub.uploadProblemTestData(new UploadTestDataStreamObserver(this));
        var problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);

        AtomicReference<String> hash = new AtomicReference<>(), testDataId = new AtomicReference<>();

        problemService.findProblemById(submission.getProblemId())
                // get problem
                .flatMap(p -> {
                    testDataId.set(p.getTestDataId());
                    return p.getTestDataHash();
                })
                // get test data hash
                .flatMapMany(h -> {
                    hash.set(h);
                    try {
                        return problemService.findRawProblemTestDataFlux(testDataId.get());
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

    // for sorting algorithm
    @Override
    public int compareTo(SmoothieRunner runner) {
        club.bayview.smoothieweb.SmoothieRunner.ServiceHealth s1 = null, s2 = null;
        try {
            s1 = this.getHealth();
        } catch (StatusRuntimeException ignored) {
        }
        try {
            s2 = runner.getHealth();
        } catch (StatusRuntimeException ignored) {
        }

        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return 1;
        if (s2 == null) return -1;

        return Long.compare(s1.getNumOfTasksInQueue(), s2.getNumOfTasksToBeDone());
    }
}
