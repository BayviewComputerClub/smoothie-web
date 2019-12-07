package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieRunner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
    public static class ProblemBatchCase implements Comparable<ProblemBatchCase> {
        private int batchNum, caseNum, scoreWorth;
        private String input, expectedOutput;

        public ProblemBatchCase(int batchNum, int caseNum, String input, String expectedOutput) {
            this.batchNum = batchNum;
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

    private Collection<ProblemBatchCase> testData;

    private String problemStatement;

    private Collection<ObjectId> submissions;

    private boolean allowPartial;
    private int totalScoreWorth;

    private int rateOfAC, usersSolved;
    private long timeCreated;

    public ProblemLimits getLimit(String lang) {
        for (ProblemLimits l : getLimits()) {
            if (l.getLang().equals(lang)) return l;
        }
        return null;
    }

    public HashMap<Integer, List<ProblemBatchCase>> getTestDataSorted() {
        HashMap<Integer, List<ProblemBatchCase>> group = new HashMap<>();
        for (ProblemBatchCase c : testData) {
            group.computeIfAbsent(c.getBatchNum(), k -> new ArrayList<>(Arrays.asList(c)));
            if (group.get(c.getBatchNum()) != null) {
                group.get(c.getBatchNum()).add(c);
            }
        }

        for (var list : group.values()) Collections.sort(list);
        return group;
    }

    public SmoothieRunner.Problem getGRPCObject(String language) {

        var test = getTestDataSorted();
        int max = 0;
        for (int i : test.keySet()) max = Math.max(max, i);

        List<SmoothieRunner.ProblemBatch> batch = new ArrayList<>();
        for (int i = 0; i <= max+1; i++) batch.add(SmoothieRunner.ProblemBatch.newBuilder().build());

        ProblemLimits limit = getLimits().get(0);
        for (ProblemLimits l : getLimits()) {
            if (l.getLang().equals(language)) limit = l;
        }

        for (int i : test.keySet()) {
            List<SmoothieRunner.ProblemBatchCase> cases = new ArrayList<>();
            for (var c : test.get(i)) {

                cases.add(SmoothieRunner.ProblemBatchCase.newBuilder()
                        .setInput(c.getInput())
                        .setExpectedAnswer(c.getExpectedOutput())
                        .setTimeLimit(limit.getTimeLimit())
                        .setMemLimit(limit.getMemoryLimit())
                        .build());
            }

            batch.set(i, SmoothieRunner.ProblemBatch.newBuilder().addAllCases(cases).build());
        }

        return SmoothieRunner.Problem.newBuilder()
                .setProblemID(1) // TODO
                .setTestCasesHashCode(0) // TODO
                .addAllTestBatches(batch)
                .setGrader(SmoothieRunner.ProblemGrader.newBuilder()
                        .setType("strict") // TODO
                        .build())
                .build();
    }


}
