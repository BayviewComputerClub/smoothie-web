package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a programming problem on the site.
 */

@Document(collation =  "{ 'locale' : 'en_US', 'strength': 2 }") // indexes case insensitive
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

    @Indexed(unique = true)
    private String prettyName;

    private List<ProblemLimits> limits;

    private String testDataId;

    private String problemStatement;

    private boolean allowPartial;
    private int totalPointsWorth;

    private int rateOfAC, usersSolved;
    private long timeCreated;

    public Mono<TestData> getTestData() {
        try {
            return SmoothieWebApplication.context.getBean(SmoothieProblemService.class).findProblemTestData(getTestDataId());
        } catch (Exception e) {
            e.printStackTrace();
            return Mono.error(e);
        }
    }

    public Mono<List<List<Submission.SubmissionBatchCase>>> getSubmissionBatchCases() {
        List<List<Submission.SubmissionBatchCase>> l = new ArrayList<>();

        return getTestData().flatMap(testData -> {
            for (var cases : testData.getTestData()) {
                l.add(new ArrayList<>());
                for (var c : cases) {
                    l.get(l.size()-1).add(new Submission.SubmissionBatchCase(c));
                }
            }
            return Mono.just(l);
        });
    }

    public Mono<List<SmoothieRunner.ProblemBatch>> getGRPCBatches(ProblemLimits limit) {
        // convert to grpc test cases
        List<SmoothieRunner.ProblemBatch> batches = new ArrayList<>();

        return getTestData().flatMap(testData -> {
            for (var batch : testData.getTestData()) {
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
            return Mono.just(batches);
        });
    }

    public ProblemLimits getProblemLimitForLang(String language) {
        // get limit
        ProblemLimits limit = null;
        for (ProblemLimits l : getLimits()) {
            if (l.getLang().equals(language)) limit = l;

            if (limit == null && l.getLang().equals(JudgeLanguage.ALL.getName())) { // default with all
                limit = l;
            }
        }
        return limit;
    }

    public Mono<SmoothieRunner.Problem> getGRPCObject(String language) {
        // get final grpc object
        return getGRPCBatches(getProblemLimitForLang(language)).flatMap(batches -> Mono.just(SmoothieRunner.Problem.newBuilder()
            .setProblemID(id)
            .setTestCasesHashCode(0) // TODO
            .addAllTestBatches(batches)
            .setGrader(SmoothieRunner.ProblemGrader.newBuilder()
                    .setType("strict") // TODO
                    .build())
            .build()));
    }


}
