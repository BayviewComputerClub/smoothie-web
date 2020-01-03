package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.index.Indexed;

import java.util.List;

@Document
@Getter
@Setter
public class QueuedSubmission {

    @Id
    String id;

    @Indexed
    String submissionId;

    List<String> requestedRunnerIds; // leave empty for all
}
