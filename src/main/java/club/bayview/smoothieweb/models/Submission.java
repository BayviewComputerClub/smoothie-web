package club.bayview.smoothieweb.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents a submission made by a user for a problem.
 */

@Document
public class Submission {

    static class SubmissionBatchCase {
        public static final String AWAITING_RESULTS = "AR";

        private long batchNumber, caseNumber;
        String resultCode, error;
        private double time, memUsage;

        public SubmissionBatchCase() {
            resultCode = AWAITING_RESULTS;
        }

        public boolean isAwaitingResults() {
            return resultCode.equals(AWAITING_RESULTS);
        }

        public long getBatchNumber() {
            return batchNumber;
        }

        public void setBatchNumber(long batchNumber) {
            this.batchNumber = batchNumber;
        }

        public long getCaseNumber() {
            return caseNumber;
        }

        public void setCaseNumber(long caseNumber) {
            this.caseNumber = caseNumber;
        }

        public String getResultCode() {
            return resultCode;
        }

        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public double getTime() {
            return time;
        }

        public void setTime(double time) {
            this.time = time;
        }

        public double getMemUsage() {
            return memUsage;
        }

        public void setMemUsage(double memUsage) {
            this.memUsage = memUsage;
        }
    }

    @Id
    private String id;

    private JudgeLanguage lang;

    @DBRef
    private User user;

    @DBRef
    private Problem problem;

    @DBRef
    private Runner runner;

    private String code;
    private Long timeSubmitted;

    private String compileError;
    private ArrayList<SubmissionBatchCase> batchCases;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public String getId() {
        return id;
    }

    public JudgeLanguage getLang() {
        return lang;
    }

    public void setLang(JudgeLanguage lang) {
        this.lang = lang;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public Runner getRunner() {
        return runner;
    }

    public void setRunner(Runner runner) {
        this.runner = runner;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getTimeSubmitted() {
        return timeSubmitted;
    }

    public void setTimeSubmitted(Long timeSubmitted) {
        this.timeSubmitted = timeSubmitted;
    }

    public String getCompileError() {
        return compileError;
    }

    public void setCompileError(String compileError) {
        this.compileError = compileError;
    }

    public ArrayList<SubmissionBatchCase> getBatchCases() {
        return batchCases;
    }

    public void setBatchCases(ArrayList<SubmissionBatchCase> batchCases) {
        this.batchCases = batchCases;
    }

}
