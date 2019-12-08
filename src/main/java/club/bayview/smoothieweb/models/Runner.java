package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.SmoothieRunner;
import club.bayview.smoothieweb.services.SmoothieRunnerService;
import io.grpc.ConnectivityState;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;

@Document
@Getter
@Setter
public class Runner {

    @Id
    private String id;

    private String name;
    private String description;

    private String host;
    private int port;

    Collection<JudgeLanguage> languagesSupported;

    private SmoothieRunnerService getSmoothieRunnerService() {
        return SmoothieWebApplication.context.getBean(SmoothieRunnerService.class);
    }

    public ConnectivityState getState() {
        return getSmoothieRunnerService().getSmoothieRunner(id) == null ? null : getSmoothieRunnerService().getSmoothieRunner(id).getStatus();
    }

    public SmoothieRunner getSmoothieRunner() {
        return getSmoothieRunnerService().getSmoothieRunner(id);
    }

}
