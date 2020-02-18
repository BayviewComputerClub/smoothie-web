package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.models.testdata.StoredTestData;
import club.bayview.smoothieweb.util.Verdict;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * Represents a submission made by a user for a problem.
 */

@Document
@Getter
@Setter
@NoArgsConstructor
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

        public SubmissionBatchCase(StoredTestData.TestDataBatchCase c) {
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

    @Indexed
    private String userId;

    @Indexed
    private String problemId;

    @Indexed
    private String contestId;

    private String runnerId;

    private String code;
    private Long timeSubmitted;

    private String compileError;
    private List<List<SubmissionBatchCase>> batchCases;

    private String verdict = Verdict.AR.toString();

    private boolean judgingCompleted;

    private double points, maxPoints;

    /**
     * Check if a given authentication has permission to view the contents of the submission.
     * @param auth the Authentication object
     * @param problem the problem that the submission belongs to
     * @return whether or not the authentication has permission to view
     */

    public boolean hasPermissionToView(Authentication auth, Problem problem) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User)) {
            return false;
        }

        // admins are automatically allowed to see
        if (auth.getAuthorities().contains(Role.ROLE_ADMIN)) {
            return true;
        }

        User user = (User) auth.getPrincipal();

        // if it is the user that submitted
        if (getUserId().equals(user.getId())) {
            return true;
        }
        // if the user has solved the problem
        if (user.getSolved().contains(getProblemId())) {
            return true;
        }

        // if the problem does not exist
        if (problem == null) {
            return true;
        }
        // if the user is an editor
        if (problem.getEditorIds().contains(user.getId())) {
            return true;
        }
        // deny otherwise
        return false;
    }

    /**
     * Determines the verdict given based on the submission information.
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

    /**
     * Calculate the amount of points that should be given for the submission.
     * Should be run AFTER the verdict is determined.
     * @param p the problem that the submission is for
     */

    public void determinePoints(Problem p) {
        if (p.isAllowPartial()) {
            if (batchCases.size() == p.getProblemBatches().size()) {
                boolean perfect = true;

                // loop over each batch
                for (int i = 0; i < batchCases.size(); i++) {
                    boolean isAC = true;
                    for (var c : batchCases.get(i)) {
                        if (!c.getResultCode().equals(Verdict.AC.toString())) {
                            isAC = false;
                            perfect = false;
                            break;
                        }
                    }
                    // determine if the batch has passed
                    if (isAC) {
                        points += (double)p.getProblemBatches().get(i).getPointsWorth() / 100 * p.getTotalPointsWorth(); // TODO round maybe
                    }
                }

                // if all cases passed, just don't use rounding and give full points
                if (perfect) {
                    points = p.getTotalPointsWorth();
                }
            }
            // if the test data has changed since original judging, just leave the points as is
        } else {
            points = verdict.equals(Verdict.AC.toString()) ? p.getTotalPointsWorth() : 0;
        }
    }
}
