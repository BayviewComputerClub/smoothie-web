package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.index.Indexed;

import java.util.List;

/**
 * Represents a blog post
 */

@Document(collation = "{ 'locale' : 'en_US', 'strength': 2 }") // indexes case insensitive
@Getter
@Setter
public class Post {

    @Id
    private String id;

    @Indexed
    private List<String> userCreatorIds;

    @Indexed
    private String slug, userGroupId;

    @Indexed
    private boolean isGlobalScope;

    private String name, content;

    private long created, lastEdited;

}
