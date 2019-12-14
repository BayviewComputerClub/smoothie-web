package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import lombok.*;
import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a programming problem on the site.
 */

@Document
@Getter
@Setter
@NoArgsConstructor
public class Problem {

    // problem requirements for each language (or just the ALL language to apply to all)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemLimits {
        private String lang;
        private double timeLimit, memoryLimit; // time limit in seconds, memory limit in mb
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @ToString
    public static class ProblemBatchCase implements Comparable<ProblemBatchCase>, Serializable {
        private int batchNum, caseNum, scoreWorth;
        private String input, expectedOutput;

        public ProblemBatchCase(int batchNum, int caseNum, String input, String expectedOutput) {
            this.batchNum = batchNum;
            this.caseNum = caseNum;
            this.input = input;
            this.expectedOutput = expectedOutput;
        }

        public ProblemBatchCase(int batchNum, int caseNum, int scoreWorth, String input, String expectedOutput) {
            this(batchNum, caseNum, input, expectedOutput);
            this.scoreWorth = scoreWorth;
        }

        @Override
        public int compareTo(ProblemBatchCase problemBatchCase) {
            return caseNum > problemBatchCase.caseNum ? 1 : -1;
        }
    }


    @Id
    private String id;
    @Indexed(unique = true)
    private String name;

    private String prettyName;

    private List<ProblemLimits> limits;

    private String testDataId;

    private String problemStatement;

    private boolean allowPartial;
    private int totalScoreWorth;

    private int rateOfAC, usersSolved;
    private long timeCreated;

    public TestData getTestData() {
        // TODO
        try {
            return SmoothieWebApplication.context.getBean(SmoothieProblemService.class).findProblemTestData(getTestDataId()).block();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<List<Submission.SubmissionBatchCase>> getSubmissionBatchCases() {
        List<List<Submission.SubmissionBatchCase>> l = new ArrayList<>();

        for (var cases : getTestData().testData) {
            l.add(new ArrayList<>());
            for (var c : cases) {
                l.get(l.size()-1).add(new Submission.SubmissionBatchCase(c));
            }
        }
        return l;
    }

    public SmoothieRunner.Problem getGRPCObject(String language) {

        // get limit
        ProblemLimits limit = null;
        for (ProblemLimits l : getLimits()) {
            if (l.getLang().equals(language)) limit = l;

            if (limit == null && l.getLang().equals(JudgeLanguage.ALL.getName())) { // default with all
                limit = l;
            }
        }

        // convert to grpc test cases
        List<SmoothieRunner.ProblemBatch> batches = new ArrayList<>();
        for (var batch : getTestData().testData) {
            SmoothieRunner.ProblemBatch.Builder b = SmoothieRunner.ProblemBatch.newBuilder();
            for (var c : batch) {
                b.addCases(SmoothieRunner.ProblemBatchCase.newBuilder()
                        .setInput(c.getInput())
                        .setExpectedAnswer(c.getExpectedOutput())
                        .setTimeLimit(limit.getTimeLimit())
                        .setMemLimit(limit.getMemoryLimit())
                        .build());
            }
            batches.add(b.build());
        }

        // get final grpc object
        return SmoothieRunner.Problem.newBuilder()
                .setProblemID(id)
                .setTestCasesHashCode(0) // TODO
                .addAllTestBatches(batches)
                .setGrader(SmoothieRunner.ProblemGrader.newBuilder()
                        .setType("strict") // TODO
                        .build())
                .build();
    }


}
