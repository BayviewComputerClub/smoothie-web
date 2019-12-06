package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Collection;

/**
 * Represents a programming problem on the site.
 */

@Document
@Getter
@Setter
public class Problem {

    // problem requirements for each language (or just the ALL language to apply to all)
    @Getter
    @Setter
    public static class ProblemLimits {
        private String lang;
        private double timeLimit, memoryLimit; // time limit in seconds, memory limit in mb

        public ProblemLimits() {}

        public ProblemLimits(String lang, double timeLimit, double memoryLimit) {
            this.lang = lang;
            this.timeLimit = timeLimit;
            this.memoryLimit = memoryLimit;
        }

    }

    @Getter
    @Setter
    public static class ProblemBatchCase {
        private int batchNum, caseNum, scoreWorth;
        private String input, expectedOutput;

        public ProblemBatchCase() {}

        public ProblemBatchCase(int batchNum, int caseNum, String input, String expectedOutput) {
            this.batchNum = batchNum;
            this.input = input;
            this.expectedOutput = expectedOutput;
        }

        public ProblemBatchCase(int batchNum, int caseNum, int scoreWorth, String input, String expectedOutput) {
            this(batchNum, caseNum, input, expectedOutput);
            this.scoreWorth = scoreWorth;
        }
    }


    @Id
    private String id;
    @Indexed(unique = true)
    private String name;

    private String prettyName;

    private Collection<ProblemLimits> limits;

    private Collection<ProblemBatchCase> testData;

    private String problemStatement;

    private Collection<ObjectId> submissions;

    private boolean allowPartial;
    private int totalScoreWorth;

    private int rateOfAC, usersSolved;
    private long timeCreated;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Problem() {
        super();
    }

}
