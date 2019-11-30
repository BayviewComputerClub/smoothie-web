package club.bayview.smoothieweb.services;

import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;

@Service
public class SmoothieRunnerService {

    public void initializeRunnerConnection(String host, int port) {
        ManagedChannelBuilder<?> channelBuilder;
    }

}
