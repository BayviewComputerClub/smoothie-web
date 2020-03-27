package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.util.Verdict;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.redis.core.index.Indexed;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a submission made by a user for a problem.
 */

@Document
@Getter
@Setter
@NoArgsConstructor
public class Submission {

    public enum SubmissionStatus {
        COMPLETE, // verdict was given
        JUDGING, // currently judging
        REJUDGE, // rejudge was requested (should wipe results)
        CANCELLED, // submission judging was cancelled
        AWAITING_RUNNER, // waiting for runner to pick up job
    }

    @Getter
    @Setter
    public static class SubmissionBatch {
        long pointsAwarded, maxPoints;
        List<SubmissionBatchCase> cases = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class SubmissionBatchCase {
        private long batchNumber, caseNumber;
        String resultCode, error;
        private double time, memUsage;

        public SubmissionBatchCase() {
            resultCode = Verdict.AR.toString();
        }

        public SubmissionBatchCase(int batchNum, int caseNum) {
            this();
            this.batchNumber = batchNum;
            this.caseNumber = caseNum;
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

    private SubmissionStatus status;

    private String code;
    private Long timeSubmitted;

    private String compileError;
    private List<SubmissionBatch> batchCases;

    private String verdict = Verdict.AR.toString();

    private boolean judgingCompleted;

    private int points, maxPoints;

    /**
     * Check if a given authentication has permission to view the contents of the submission.
     *
     * @param auth    the Authentication object
     * @param problem the problem that the submission belongs to
     * @return whether or not the authentication has permission to view
     */

    public boolean hasPermissionToView(Authentication auth, Problem problem) {
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User))
            return false;

        User user = (User) auth.getPrincipal();

        // admins are automatically allowed to see
        if (user.getRoles().contains(Role.ROLE_ADMIN))
            return true;

        // if it is the user that submitted
        if (getUserId().equals(user.getId()))
            return true;

        // if the user has solved the problem
        if (user.getSolved().contains(getProblemId()))
            return true;

        // if the problem does not exist
        if (problem == null)
            return true;

        // if the user is an editor
        if (problem.getEditorIds() != null && problem.getEditorIds().contains(user.getId()))
            return true;

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
            for (var c : batch.cases) {
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
     *
     * @param p the problem that the submission is for
     */

    public void determinePoints(Problem p) {
        points = 0;
        if (p.isAllowPartial()) { // allow partial - every case in each batch is evaluated
            for (var batch : batchCases) {
                batch.pointsAwarded = 0;
                for (var c : batch.cases) {
                    // add a point if case passed
                    if (c.getResultCode().equals(Verdict.AC.toString())) {
                        batch.pointsAwarded++;
                    }
                }
                // correct the points awarded to the scale of maxPoints
                batch.pointsAwarded = Math.round(((double) batch.pointsAwarded) / batch.cases.size() * batch.maxPoints);
                points += batch.pointsAwarded;
            }
        } else { // each batch is either pass (get all points for batch) or fail (0 points)
            for (var batch : batchCases) {
                batch.pointsAwarded = batch.maxPoints;
                for (var c : batch.cases) {
                    // give no points if a case fails
                    if (!c.getResultCode().equals(Verdict.AC.toString())) {
                        batch.pointsAwarded = 0;
                        break;
                    }
                }
                points += batch.pointsAwarded;
            }
        }
    }
}
