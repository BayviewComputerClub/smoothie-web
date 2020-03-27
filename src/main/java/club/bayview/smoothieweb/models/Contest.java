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
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.Authentication;
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

        private double points; // must be refreshed before sorting
        private long timeStart; // when the user clicked "join" on the contest
        List<ContestUserSubmission> bestSubmissions; // submission to show on leaderboard; each index is the contestProblemNumber (it's in order)

        private String userId;

        /**
         * Get a default contest user using user information.
         * Used for users initially joining the contest.
         *
         * @param contest the contest being joined
         * @param user the user
         * @return the contest user to be added to the contest
         */

        public static ContestUser getDefault(Contest contest, User user) {
            ContestUser u = new ContestUser();
            u.setPoints(0);
            u.setTimeStart(System.currentTimeMillis());
            u.setUserId(user.getId());
            u.setBestSubmissions(new ArrayList<>());

            for (ContestProblem cp : contest.getContestProblemsInOrder()) {
                ContestUserSubmission cus = new ContestUserSubmission();
                cus.setTimeSubmitted(0);
                cus.setProblemId(cp.getProblemId());
                cus.setPoints(0);
                cus.setMaxPoints(cp.getTotalPointsWorth());
                u.getBestSubmissions().add(cus);
            }
            return u;
        }

        public long getSubmissionEndTime(Contest c) {
            if (c.getSubmissionPeriod() == 0) {
                return c.getTimeEnd();
            } else {
                return Math.min(c.getTimeEnd(), c.getSubmissionPeriod()*1000*60 + getTimeStart());
            }
        }

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

    private String description;

    // <problemId, contestproblem>
    private HashMap<String, ContestProblem> contestProblems = new HashMap<>();

    // in unix time
    private long timeStart, // when the contest allows people to join and submit (millisecond epoch)
            timeEnd, // when the contest closes submissions (millisecond epoch)
            submissionPeriod, // contest length (minutes) for when people can submit from when they first "join" the contest; set to 0 to allow submissions throughout the contest length
            timeCreated; // when the contest was created

    private boolean enabled,
            visibleToPublic,
            timeMatters, // time adds points (breaks ties)
            hiddenLeaderBoard; // TODO

    private List<String> testerUserIds = new ArrayList<>();
    private List<String> juryUserIds = new ArrayList<>();
    private List<String> editorUserIds = new ArrayList<>();

    // <userId, contest information>
    private HashMap<String, ContestUser> participants = new HashMap<>();

    // [ranking][users in that rank]
    // uses user ids
    private List<List<String>> leaderBoard = new ArrayList<>();

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    enum ContestStatus {
        DISABLED("Disabled"),
        AWAITING_START("Awaiting Start"),
        ONGOING("Ongoing"),
        FINISHED("Finished");

        @Getter
        String pretty;
        ContestStatus(String pretty) {
            this.pretty = pretty;
        }
    }

    public ContestStatus getStatus() {
        if (!isEnabled())
            return ContestStatus.DISABLED;

        long currentTime = System.currentTimeMillis();
        if (currentTime < getTimeStart())
            return ContestStatus.AWAITING_START;

        if (currentTime > getTimeEnd())
            return ContestStatus.FINISHED;

        return ContestStatus.ONGOING;
    }

    public List<ContestProblem> getContestProblemsInOrder() {
        List<ContestProblem> list = new ArrayList<>(contestProblems.values());
        Collections.sort(list);
        return list;
    }

    // generate sorted leaderboard
    // be sure to save contest object after
    public void updateLeaderBoard() {
        leaderBoard = new ArrayList<>();

        for (var user : participants.values()) {
            long timePenalty = user.getTimePenalty();

            if (leaderBoard.isEmpty()) { // first element
                leaderBoard.add(new ArrayList<>(Arrays.asList(user.getUserId())));
                continue;
            }

            boolean inserted = false;
            // insert user into leaderboard
            for (int i = 0; i < leaderBoard.size(); i++) { // assume every thing has one element
                var l = leaderBoard.get(i);
                var userCompare = participants.get(l.get(0));

                if (userCompare.getPoints() == user.getPoints()) { // same points
                    if (isTimeMatters()) { // compare time penalty
                        long timePenaltyCompare = userCompare.getTimePenalty();
                        if (timePenalty == timePenaltyCompare) { // same time penalty, add to same list
                            l.add(user.getUserId());
                            inserted = true;
                            break;
                        } else if (timePenalty < timePenaltyCompare) { // time penalty is smaller, it takes the place
                            leaderBoard.add(i, new ArrayList<>(Arrays.asList(user.getUserId())));
                            inserted = true;
                            break;
                        }
                        // otherwise, continue
                    } else { // if time does not matter, just add to the same group
                        l.add(user.getUserId());
                        inserted = true;
                        break;
                    }
                } else if (userCompare.getPoints() < user.getPoints()) { // insert before this one
                    leaderBoard.add(i, new ArrayList<>(Arrays.asList(user.getUserId())));
                    inserted = true;
                    break;

                }
            }

            // add to end of leaderboard
            if (!inserted) {
                leaderBoard.add(new ArrayList<>(Arrays.asList(user.getUserId())));
            }
        }
    }

    // refresh a participant's information
    // be sure to save contest object
    public Mono<Contest> updateParticipant(String userId) {
        List<String> checkedSubmissions = Arrays.asList(Verdict.AC.toString(), Verdict.WA.toString(), Verdict.MLE.toString(), Verdict.TLE.toString());

        HashMap<String, Submission> m = new HashMap<>();

        return SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class)
                .findSubmissionsByUserForContest(userId, this.getId(), Pageable.unpaged())
                .collectList() // can't use flux directly because the order of the comparisons may become messed up
                .doOnNext(submissions -> {

                    // TODO do this separately, and individually per user when a submission is done for the user
                    // TODO prevent data race, use cache
                    // TODO submissions could be for problems not in contest (contest was edited)
                    for (var s : submissions) {
                        // ignore AR (awaiting) submissions
                        if (!checkedSubmissions.contains(s.getVerdict())) {
                            continue;
                        }
                        // add submissions to map with best points and submitted time
                        Submission compare = m.get(s.getProblemId());
                        if (compare == null || // if the submission has not been added yet
                                compare.getPoints() < s.getPoints() || // the points calculated for the submission are higher than what is currently in cache
                                compare.getTimeSubmitted() > s.getTimeSubmitted() || // take older submission
                                (!compare.getVerdict().equals("AC") && s.getVerdict().equals("AC"))) { // if the old submission was not AC

                            m.put(s.getProblemId(), s);
                        }
                    }

                    // get contest user object and clear old data
                    ContestUser u;
                    if (participants.containsKey(userId)) {
                        u = participants.get(userId);
                    } else {
                        u = new ContestUser();
                        u.setTimeStart(System.currentTimeMillis());
                    }
                    u.getBestSubmissions().clear();;
                    u.setPoints(0);

                    // loop over contest problems in order and add to best submissions
                    for (ContestProblem cp : getContestProblemsInOrder()) {
                        var cus = new ContestUserSubmission();
                        if (m.containsKey(cp.getProblemId())) { // submission for problem has been recorded
                            var sub = m.get(cp.getProblemId());
                            cus.setMaxPoints(cp.getTotalPointsWorth());
                            cus.setPoints((double)sub.getPoints() / sub.getMaxPoints() * cp.getTotalPointsWorth()); // convert from problem points to contest points
                            cus.setProblemId(sub.getProblemId());
                            cus.setTimeSubmitted(sub.getTimeSubmitted());
                        } else { // no submissions yet
                            cus.setMaxPoints(cp.getTotalPointsWorth());
                            cus.setPoints(0);
                            cus.setProblemId(cp.getProblemId());
                            cus.setTimeSubmitted(0);
                        }

                        // add points to contest user and add submission
                        u.setPoints(u.getPoints() + cus.getPoints());
                        u.getBestSubmissions().add(cus);
                    }

                    participants.put(userId, u);
                })
                .then(Mono.just(this));
    }

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ Permissions ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Whether or not a user can view a contest at all.
     * This does not include viewing problems, and submitting to them.
     *
     * @param auth the authentication session
     * @return whether or not the authentication has permission to view the contest
     */

    public boolean hasPermissionToView(Authentication auth) {
        if (isVisibleToPublic()) return true;

        // not logged in
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User))
            return false;

        User u = (User) auth.getPrincipal();

        // is admin
        if (u.getRoles().contains(Role.ROLE_ADMIN))
            return true;

        // is a tester or an editor or jury
        if (testerUserIds.contains(u.getId()) || editorUserIds.contains(u.getId()) || juryUserIds.contains(u.getId()))
            return true;

        return false;
    }

    /**
     * Whether or not a user can view problem statements in the contest.
     *
     * @param auth the authentication session
     * @return whether or not the authentication has permission to view the contest problems
     */

    public boolean hasPermissionToViewProblems(Authentication auth) {
        if (!hasPermissionToView(auth))
            return false;

        // not logged in
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User))
            return false;

        // users that can submit can also see problems
        if (hasPermissionToSubmit(auth))
            return true;

        // if the contest has ended, everyone can view problems
        return System.currentTimeMillis() > getTimeEnd();
    }

    /**
     * Whether or not a user can submit to problems in the contest.
     *
     * @param auth the authentication session
     * @return whether or not the authentication has permission to submit to problems in the contest
     */

    public boolean hasPermissionToSubmit(Authentication auth) {
        if (!hasPermissionToView(auth))
            return false;

        // not logged in
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User))
            return false;

        long currentTime = System.currentTimeMillis();

        User u = (User) auth.getPrincipal();

        // is admin
        if (u.getRoles().contains(Role.ROLE_ADMIN))
            return true;

        // check if user is in the contest mode
        if (!u.getContestId().equals(getId()))
            return false;

        // if user is not a contest participant
        if (!participants.containsKey(u.getId()))
            return false;

        // if the contest has ended
        if (getStatus() != ContestStatus.ONGOING)
            return false;

        ContestUser cu  = participants.get(u.getId());

        // if user is out of time
        if (currentTime > cu.getSubmissionEndTime(this))
            return false;

        return true;
    }

    /**
     * Whether or not a user can manage a contest (answer clarifications, view statistics and submissions).
     * This does not include edit capabilities.
     *
     * @param auth the authentication session
     * @return whether or not the authentication has permission to manage the contest
     */

    public boolean hasPermissionToManage(Authentication auth) {
        if (!hasPermissionToView(auth))
            return false;

        // not logged in
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User))
            return false;

        User u = (User) auth.getPrincipal();
        // is admin
        if (u.getRoles().contains(Role.ROLE_ADMIN))
            return true;

        // is editor or jury
        return editorUserIds.contains(u.getId()) || juryUserIds.contains(u.getId());
    }

    /**
     * Whether or not a user has permission to edit the contest.
     *
     * @param auth the authentication session
     * @return whether or not the authentication has permission to edit this contest
     */

    public boolean hasPermissionToEdit(Authentication auth) {
        if (!hasPermissionToManage(auth))
            return false;

        User u = (User) auth.getPrincipal();
        // is admin
        if (u.getRoles().contains(Role.ROLE_ADMIN))
            return true;

        // is editor
        return editorUserIds.contains(u.getId());
    }
}
