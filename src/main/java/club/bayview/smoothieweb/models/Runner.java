package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.submissions.SmoothieRunner;
import club.bayview.smoothieweb.services.submissions.SmoothieRunnerService;
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
        return getSmoothieRunner() == null ? null : getSmoothieRunner().getStatus();
    }

    public club.bayview.smoothieweb.SmoothieRunner.ServiceHealth getHealth() {
        return getSmoothieRunner() == null ? null : getSmoothieRunner().getHealth();
    }

    public SmoothieRunner getSmoothieRunner() {
        return getSmoothieRunnerService().getSmoothieRunner(id);
    }

}
