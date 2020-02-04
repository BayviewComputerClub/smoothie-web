package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import club.bayview.smoothieweb.util.Verdict;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Represents a site-wide programming contest.
 */

@Document(collation = "{ 'locale' : 'en_US', 'strength': 2 }") // indexes case insensitive
@Getter
@Setter
@NoArgsConstructor
public class Contest {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContestProblem implements Comparable<ContestProblem> {
        private int contestProblemNumber; // starts at zero
        private String problemId; // problem referred to

        private String customName, colourHex;
        private int totalPointsWorth; // partials will multiply this

        @Override
        public int compareTo(ContestProblem o) {
            return contestProblemNumber - o.contestProblemNumber;
        }
    }


    // best submission for a problem by a user
    @Getter
    @Setter
    public static class ContestUserSubmission {
        private String problemId;

        private long timeSubmitted; // unix time for time submitted
        private double points,// points obtained on problem (on the contest type points, not on original problem type points)
                maxPoints; // maximum amount of points possible
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContestUser {

        // must be refreshed before sorting
        private double points;
        List<ContestUserSubmission> bestSubmissions; // submission to show on leaderboard; each index is the contestProblemNumber

        private String userId;

        public long getTimePenalty() {
            long penalty = 0;
            for (var s : bestSubmissions) {
                penalty += s.getTimeSubmitted();
            }
            return penalty;
        }
    }

    @Id
    private String id;

    // used in links, no spaces
    @Indexed(unique = true)
    private String name;

    // spaces allowed
    @Indexed(unique = true)
    private String prettyName;

    private HashMap<String, ContestProblem> contestProblems; // <problemId, contestproblem>

    // in unix time
    private long timeStart, // when the contest allows people to join and submit
            timeEnd, // when the contest closes submissions
            submissionPeriod; // contest length for when people can submit from when they first "join" the contest; set to 0 to allow submissions throughout the contest length

    private boolean enabled,
            visibleToPublic,
            timeMatters; // time adds points (breaks ties)

    private List<String> testerUserIds;
    private List<String> editorUserIds;

    private HashMap<String, ContestUser> participants; // <userId, contest information>

    private List<List<ContestUser>> leaderBoard; // [ranking][users in that rank]

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private SmoothieProblemService problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);
    private SmoothieSubmissionService submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);

    public List<ContestProblem> getContestProblemsInOrder() {
        List<ContestProblem> list = new ArrayList<>(contestProblems.values());
        Collections.sort(list);
        return list;
    }

    // use insertion sort sorta to make leader board
    public void updateLeaderBoard() {
        leaderBoard = new ArrayList<>();
        for (var user : participants.values()) {
            long timePenalty = user.getTimePenalty();

            if (leaderBoard.isEmpty()) { // first element
                leaderBoard.add(new ArrayList<>(Arrays.asList(user)));
                continue;
            }

            // insert user into leaderboard in a dumb way
            for (int i = 0; i < leaderBoard.size(); i++) { // assume every thing has one element
                var l = leaderBoard.get(i);

                if (l.get(0).getPoints() == user.getPoints()) { // same points
                    if (isTimeMatters()) { // compare time penalty
                        long timePenaltyCompare = l.get(0).getTimePenalty();
                        if (timePenalty == timePenaltyCompare) { // same time penalty, add to same list
                            l.add(user);
                        } else if (timePenalty < timePenaltyCompare) { // time penalty is smaller, it takes the place
                            leaderBoard.add(i, new ArrayList<>(Arrays.asList(user)));
                        }
                        // otherwise, continue
                    } else {
                        l.add(user);
                    }
                } else if (l.get(0).getPoints() < user.getPoints()) { // insert before this one
                    leaderBoard.add(i, new ArrayList<>(Arrays.asList(user)));
                }

            }
        }
    }

    // refresh a participant's information
    public Mono<Void> updateParticipant(String userId) {
        List<String> checkedSubmissions = Arrays.asList(Verdict.AC.toString(), Verdict.WA.toString(), Verdict.MLE.toString(), Verdict.TLE.toString());

        HashMap<String, Submission> m = new HashMap<>();

        return submissionService.findSubmissionsByUserForContest(userId, this.getId())
                .collectList() // can't use flux directly because the order of the comparisons may become messed up
                .doOnNext(submissions -> {
                    for (var s : submissions) {
                        // ignore AR (awaiting) submissions
                        if (!checkedSubmissions.contains(s.getVerdict())) {
                            return;
                        }
                        // add submissions to map with best points and submitted time
                        Submission compare = m.get(s.getProblemId());
                        if (compare == null || compare.getPoints() < s.getPoints() || compare.getTimeSubmitted() > s.getTimeSubmitted()) {
                            m.put(s.getProblemId(), s);
                        }
                    }

                    // form new contest user object
                    ContestUser u = new ContestUser(0, new ArrayList<>(), userId);

                    // loop over contest problems in order and add to best submissions
                    for (ContestProblem cp : getContestProblemsInOrder()) {
                        if (m.containsKey(cp.getProblemId())) { // submission for problem has been recorded
                            var sub = m.get(cp.getProblemId());
                            var cus = new ContestUserSubmission();

                            cus.setMaxPoints(cp.getTotalPointsWorth());
                            cus.setPoints(sub.getPoints()/sub.getMaxPoints()*cp.getTotalPointsWorth()); // convert from problem points to contest points
                            cus.setProblemId(sub.getProblemId());
                            cus.setTimeSubmitted(sub.getTimeSubmitted());

                            u.getBestSubmissions().add(cus);
                        } else { // no submissions yet
                            var cus = new ContestUserSubmission();
                            cus.setMaxPoints(cp.getTotalPointsWorth());
                            cus.setPoints(0);
                            cus.setProblemId(cp.getProblemId());
                            cus.setTimeSubmitted(0);

                            u.getBestSubmissions().add(cus);
                        }
                    }

                    participants.put(userId, u);
                }).then();
    }
}
