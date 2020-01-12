package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.index.Indexed;

@Document
@Getter
@Setter
public class Comment {

    @Id
    private String id;

    @Indexed
    private String userCreatorId, postId, problemId, contestId;

    private long created;

    private String content;
}
