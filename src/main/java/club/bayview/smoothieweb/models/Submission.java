package club.bayview.smoothieweb.models;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.index.Indexed;

import java.util.List;

/**
 * Represents a submission made by a user for a problem.
 */

@Document
@Getter
@Setter
public class Submission {

    @Getter
    @Setter
    public static class SubmissionBatchCase {
        public static final String AWAITING_RESULTS = "AR";

        private long batchNumber, caseNumber;
        String resultCode, error;
        private double time, memUsage;

        public SubmissionBatchCase() {
            resultCode = AWAITING_RESULTS;
        }

        public SubmissionBatchCase(Problem.ProblemBatchCase c) {
            this();
            this.batchNumber = c.getBatchNum();
            this.caseNumber = c.getCaseNum();
        }

        public boolean isAwaitingResults() {
            return resultCode.equals(AWAITING_RESULTS);
        }
    }

    @Id
    private String id;

    private String lang;

    private String userId;

    @Indexed
    private String problemId;

    private String runnerId;

    private String code;
    private Long timeSubmitted;

    private String compileError;
    private List<List<SubmissionBatchCase>> batchCases;

    private String verdict;

    private boolean judgingCompleted;

}
