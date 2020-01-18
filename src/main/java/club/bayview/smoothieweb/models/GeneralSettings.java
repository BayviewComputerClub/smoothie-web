package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Getter
@Setter
public class GeneralSettings {

    @Id
    String id;

    String siteName;

    String homeContent;

}
