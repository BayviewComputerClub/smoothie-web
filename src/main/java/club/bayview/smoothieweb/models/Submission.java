package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.util.Verdict;
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

        private long batchNumber, caseNumber;
        String resultCode, error;
        private double time, memUsage;

        public SubmissionBatchCase() {
            resultCode = Verdict.AR.toString();
        }

        public SubmissionBatchCase(Problem.ProblemBatchCase c) {
            this();
            this.batchNumber = c.getBatchNum();
            this.caseNumber = c.getCaseNum();
        }

        public boolean isAwaitingResults() {
            return resultCode.equals(Verdict.AR.toString());
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

    private String verdict = Verdict.AR.toString();

    private boolean judgingCompleted;

    /**
     * Determines the verdict based on the submission information.
     */

    public void determineVerdict() {
        // compile error
        if (compileError != null) {
            verdict = Verdict.CE.toString();
            return;
        }

        for (var batch : batchCases) {
            for (var c : batch) {
                if (!c.getResultCode().equals(Verdict.AC.toString())) {
                    verdict = c.getResultCode();
                    return;
                }
            }
        }

        // set to AC if all cases AC
        verdict = Verdict.AC.toString();
    }

}
