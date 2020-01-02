package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Represents a site-wide programming contest.
 */

@Document(collation =  "{ 'locale' : 'en_US', 'strength': 2 }") // indexes case insensitive
@Getter
@Setter
@NoArgsConstructor
public class Contest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContestProblem {
        private int contestProblemNumber; // starts at zero
        private String problemId; // problem referred to

        private String customName, colourHex;
        private int totalPointsWorth;
        private List<Integer> batchPointsWorth;

    }

    @Id
    private String id;

    // used in links, no spaces
    @Indexed(unique = true)
    private String name;

    // spaces allowed
    @Indexed(unique = true)
    private String prettyName;

    private List<ContestProblem> contestProblems;

    private long timeStart, timeEnd;

    private boolean enabled, visibleToPublic;

    private List<String> testerUserIds;
    private List<String> editorUserIds;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

}
