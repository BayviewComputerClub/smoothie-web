package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.util.NoPermissionException;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.Verdict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SubmissionVerdictService {

    @Autowired
    SmoothieUserService userService;

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieSubmissionService submissionService;

    @Autowired
    SmoothieContestService contestService;

    /**
     * Called when a submission is finished judging, and applies the final verdict.
     * @param submission submission that finished judging
     * @return mono emitted when verdict is applied and saved to database
     */
    public Mono<Void> applyVerdictToSubmission(Submission submission) {
        // store verdict and update points if necessary
        return Mono.zip(userService.findUserById(submission.getUserId()), problemService.findProblemById(submission.getProblemId()))
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(tuple -> {
                    User user = tuple.getT1();
                    Problem problem = tuple.getT2();

                    submission.determineVerdict();
                    submission.determinePoints(problem);

                    // if first time solve
                    if (submission.getVerdict().equals(Verdict.AC.toString())) {
                        if (!user.getSolved().contains(problem.getId())) {
                            user.getSolved().add(problem.getId());
                            problem.setUsersSolved(problem.getUsersSolved() + 1);
                        }
                    }

                    // update points if the submission is higher
                    if (!user.getProblemsAttempted().containsKey(problem.getId()) || user.getProblemsAttempted().get(problem.getId()) < submission.getPoints()) {
                        if (user.getProblemsAttempted().containsKey(problem.getId()) && user.getProblemsAttempted().get(problem.getId()) < submission.getPoints()) {
                            user.setPoints(user.getPoints() - user.getProblemsAttempted().get(problem.getId()));
                        }

                        user.getProblemsAttempted().put(problem.getId(), submission.getPoints());
                        user.setPoints(user.getPoints() + submission.getPoints());
                    }

                    // if there is a contest
                    if (submission.getContestId() != null) {
                        return contestService.findContestById(submission.getContestId())
                                .switchIfEmpty(Mono.error(new NoPermissionException()))
                                .flatMap(contest -> {
                                    contest.updateParticipant(user.getId());
                                    contest.updateLeaderBoard();
                                    return Mono.zip(userService.saveUser(user), problemService.saveProblem(problem), submissionService.saveSubmission(submission), contestService.saveContest(contest));
                                });
                    }

                    return Mono.zip(userService.saveUser(user), problemService.saveProblem(problem), submissionService.saveSubmission(submission));
                }).then();
    }

}
