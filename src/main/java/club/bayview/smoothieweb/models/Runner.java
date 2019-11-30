package club.bayview.smoothieweb.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;

@Document
public class Runner {

    @Id
    private String id;

    private String name;

    private String host;
    private int port;

    Collection<JudgeLanguage> languagesSupported;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Collection<JudgeLanguage> getLanguagesSupported() {
        return languagesSupported;
    }

    public void setLanguagesSupported(Collection<JudgeLanguage> languagesSupported) {
        this.languagesSupported = languagesSupported;
    }

}
