package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.testdata.StoredTestData;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a programming problem on the site.
 */

@Document(collation = "{ 'locale' : 'en_US', 'strength': 2 }") // indexes case insensitive
@Getter
@Setter
@ToString
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
    @AllArgsConstructor
    public static class ProblemBatch {
        private int batchNum, numberOfCases;
        private long pointsWorth;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TestDataWrapper {
        List<ProblemBatch> batches;
        StoredTestData.TestData testData;
    }

    @Id
    private String id;
    @Indexed(unique = true)
    private String name;

    @Indexed(unique = true)
    private String prettyName;

    private List<ProblemLimits> limits;
    private List<ProblemBatch> problemBatches;

    private String testDataId;

    private String problemStatement;

    private boolean allowPartial; // whether or not to allow partial points awarded on EACH individual batch
    private int batchPointsSum; // sum of pointsWorth of all batches
    private int scoreMultiplier; // what the points are out of on the site (contests have their own separate multiplier)

    private int rateOfAC, usersSolved;
    private long timeCreated;

    private List<String> editorIds;

    public Mono<StoredTestData.TestData> getTestData() {
        try {
            return SmoothieWebApplication.context.getBean(SmoothieProblemService.class).findProblemTestData(getTestDataId());
        } catch (Exception e) {
            e.printStackTrace();
            return Mono.error(e);
        }
    }

    public Mono<String> getTestDataHash() {
        try {
            return SmoothieWebApplication.context.getBean(SmoothieProblemService.class).findProblemTestDataHash(getTestDataId());
        } catch (Exception e) {
            e.printStackTrace();
            return Mono.error(e);
        }
    }

    public List<Submission.SubmissionBatch> getSubmissionBatchCases() {
        List<Submission.SubmissionBatch> l = new ArrayList<>();

        for (var batch : getProblemBatches()) {
            var submissionBatch = new Submission.SubmissionBatch();
            submissionBatch.setMaxPoints(batch.getPointsWorth());
            for (int i = 0; i < batch.getNumberOfCases(); i++) {
                submissionBatch.getCases().add(new Submission.SubmissionBatchCase(batch.getBatchNum(), i));
            }
            l.add(submissionBatch);
        }
        return l;
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
        ProblemLimits limit = getProblemLimitForLang(language);

        // get final grpc object
        return getTestDataHash().flatMap(hash -> Mono.just(SmoothieRunner.Problem.newBuilder()
                .setProblemId(id)
                .setTestDataHash(hash)
                .setGrader(SmoothieRunner.ProblemGrader.newBuilder()
                        .setType("strict") // TODO
                        .build())
                .setTimeLimit(limit.getTimeLimit())
                .setMemLimit(limit.getMemoryLimit())
                .build()));
    }

    // TODO
    public boolean hasPermissionToView(Authentication auth) {
        return true;
    }

}
