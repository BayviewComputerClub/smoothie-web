package club.bayview.smoothieweb.models;

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
public class Problem {

    // problem requirements for each language (or just the ALL language to apply to all)
    static class ProblemLimits {
        private JudgeLanguage lang;
        private double timeLimit, memoryLimit; // time limit in seconds, memory limit in mb

        ProblemLimits(JudgeLanguage lang, double timeLimit, double memoryLimit) {
            this.lang = lang;
            this.timeLimit = timeLimit;
            this.memoryLimit = memoryLimit;
        }

        public JudgeLanguage getLang() {
            return lang;
        }

        public double getTimeLimit() {
            return timeLimit;
        }

        public double getMemoryLimit() {
            return memoryLimit;
        }
    }

    static class ProblemBatchCase {
        private int batchNum, scoreWorth;
        private String input, expectedOutput;

        public ProblemBatchCase(int batchNum, String input, String expectedOutput) {
            this.batchNum = batchNum;
            this.input = input;
            this.expectedOutput = expectedOutput;
        }

        public ProblemBatchCase(int batchNum, int scoreWorth, String input, String expectedOutput) {
            this(batchNum, input, expectedOutput);
            this.scoreWorth = scoreWorth;
        }

        public int getBatchNum() {
            return batchNum;
        }

        public int getScoreWorth() {
            return scoreWorth;
        }

        public String getInput() {
            return input;
        }

        public String getExpectedOutput() {
            return expectedOutput;
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

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Problem() {
        super();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrettyName() {
        return prettyName;
    }

    public void setPrettyName(String prettyName) {
        this.prettyName = name;
    }

    public Collection<ProblemLimits> getLimits() {
        return limits;
    }

    public void setLimits(Collection<ProblemLimits> limits) {
        this.limits = limits;
    }

    public Collection<ProblemBatchCase> getTestData(){
        return testData;
    }

    public void setTestData(Collection<ProblemBatchCase> testData) {
        this.testData = testData;
    }

    public String getProblemStatement() {
        return problemStatement;
    }

    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
    }

    public Collection<ObjectId> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(Collection<ObjectId> submissions) {
        this.submissions = submissions;
    }

    public boolean allowPartial() {
        return allowPartial;
    }

    public void setAllowPartial(boolean allowPartial) {
        this.allowPartial = allowPartial;
    }

    public int getTotalScoreWorth() {
        return this.totalScoreWorth;
    }

    public void setTotalScoreWorth(int totalScoreWorth) {
        this.totalScoreWorth = totalScoreWorth;
    }

}
