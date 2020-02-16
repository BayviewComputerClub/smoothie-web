package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collation = "{ 'locale' : 'en_US', 'strength': 2 }") // indexes case insensitive
@Getter
@Setter
public class UserGroup {
}
