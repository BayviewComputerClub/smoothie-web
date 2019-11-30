package club.bayview.smoothieweb.models;

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

    private String host;
    private int port;

    Collection<JudgeLanguage> languagesSupported;

}
