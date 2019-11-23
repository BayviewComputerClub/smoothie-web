package club.bayview.models;

import javax.persistence.*;
import java.util.Collection;

/**
 * Represents a programming problem on the site.
 */

@Entity
public class Problem {

    // problem requirements for each language (or just the ALL language to apply to all)
    @Embeddable
    static class ProblemLimits {
        @Enumerated(EnumType.STRING)
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

    @Embeddable
    static class ProblemBatchCase {
        private int batchNum;
        private String input, expectedOutput;

        public ProblemBatchCase(int batchNum, String input, String expectedOutput) {
            this.input = input;
            this.expectedOutput = expectedOutput;
        }

        public int getBatchNum() {
            return batchNum;
        }

        public String getInput() {
            return input;
        }

        public String getExpectedOutput() {
            return expectedOutput;
        }
    }


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ElementCollection
    private Collection<ProblemLimits> limits;

    @ElementCollection
    private Collection<ProblemBatchCase> problemBatchesCases;
    private String problemStatement;

    @OneToMany(fetch = FetchType.LAZY)
    private Collection<Submission> submissions;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public Problem() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Collection<ProblemLimits> getLimits() {
        return limits;
    }

    public void setLimits(Collection<ProblemLimits> limits) {
        this.limits = limits;
    }

    public Collection<ProblemBatchCase> getTestData(){
        return problemBatchesCases;
    }

    public void setTestData(Collection<ProblemBatchCase> testData) {
        this.problemBatchesCases = testData;
    }

    public String getProblemStatement() {
        return problemStatement;
    }

    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
    }

    public Collection<Submission> getSubmissions() {
        return submissions;
    }

    public void setSubmissions(Collection<Submission> submissions) {
        this.submissions = submissions;
    }

}
