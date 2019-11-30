package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.SmoothieRunnerAPIGrpc;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmoothieRunner {

    private ManagedChannel channel;

    private String id;

    private SmoothieRunnerAPIGrpc.SmoothieRunnerAPIBlockingStub blockingStub;
    private SmoothieRunnerAPIGrpc.SmoothieRunnerAPIStub asyncStub;
    private SmoothieRunnerAPIGrpc.SmoothieRunnerAPIFutureStub futureStub;

    private Logger logger = LoggerFactory.getLogger(SmoothieRunner.class);

    public SmoothieRunner(String id, String host, int port) {
        this(id, ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    public SmoothieRunner(String id, ManagedChannelBuilder<?> channelBuilder) {
        this.id = id;
        channel = channelBuilder.build();

        blockingStub = SmoothieRunnerAPIGrpc.newBlockingStub(channel);
        asyncStub = SmoothieRunnerAPIGrpc.newStub(channel);
        futureStub = SmoothieRunnerAPIGrpc.newFutureStub(channel);

        // notifiers
        channel.notifyWhenStateChanged(ConnectivityState.READY, () -> logger.info(String.format("Runner %s changed state: READY", id)));
        channel.notifyWhenStateChanged(ConnectivityState.CONNECTING, () -> logger.info(String.format("Runner %s changed state: CONNECTING", id)));
        channel.notifyWhenStateChanged(ConnectivityState.IDLE, () -> logger.info(String.format("Runner %s changed state: IDLE", id)));
        channel.notifyWhenStateChanged(ConnectivityState.TRANSIENT_FAILURE, () -> logger.info(String.format("Runner %s changed state: TRANSIENT_FAILURE", id)));
        channel.notifyWhenStateChanged(ConnectivityState.SHUTDOWN, () -> logger.info(String.format("Runner %s changed state: SHUTDOWN", id)));
    }

    public String getId() {
        return id;
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

}
