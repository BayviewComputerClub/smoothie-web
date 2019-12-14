package club.bayview.smoothieweb.models;

import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document
@AllArgsConstructor
public class TestDataMeta {

    @Field(name = "hash")
    String hash;

}
