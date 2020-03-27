package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.*;
import club.bayview.smoothieweb.repositories.SubmissionRepository;
import club.bayview.smoothieweb.services.submissions.SmoothieQueuedSubmissionService;
import club.bayview.smoothieweb.services.submissions.SubmissionWebSocketService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SmoothieSubmissionService {

    @Autowired
    SubmissionRepository submissionRepository;

    @Autowired
    SmoothieQueuedSubmissionService queuedSubmissionService;

    @Autowired
    SubmissionWebSocketService submissionWebSocketService;

    public Mono<Submission> findSubmissionById(String id) {
        return submissionRepository.findById(id);
    }

    public Flux<Submission> findSubmissionsByProblem(String problemId, Pageable p) {
        return submissionRepository.findByProblemId(problemId, p);
    }

    public Mono<Long> countSubmissionsByProblem(String problemId) {
        return submissionRepository.countByProblemId(problemId);
    }

    public Flux<Submission> findSubmissionsByUser(String userId, Pageable p) {
        return submissionRepository.findByUserId(userId, p);
    }

    public Mono<Long> countSubmissionsByUser(String userId) {
        return submissionRepository.countByUserId(userId);
    }

    public Flux<Submission> findSubmissionsForContest(String contestId, Pageable p) {
        return submissionRepository.findByContestId(contestId, p);
    }

    public Mono<Long> countSubmissionsForContest(String contestId) {
        return submissionRepository.countByContestId(contestId);
    }

    public Flux<Submission> findSubmissionsForContestAndProblem(String contestId, String problemId, Pageable p) {
        return submissionRepository.findByContestIdAndProblemId(contestId, problemId, p);
    }

    public Mono<Long> countSubmissionsForContestAndProblem(String contestId, String problemId) {
        return submissionRepository.countByContestIdAndProblemId(contestId, problemId);
    }

    public Flux<Submission> findSubmissionsByUserForProblem(String userId, String problemId, Pageable p) {
        return submissionRepository.findByUserIdAndProblemId(userId, problemId, p);
    }

    public Mono<Long> countSubmissionsByUserAndProblem(String userId, String problemId) {
        return submissionRepository.countByUserIdAndProblemId(userId, problemId);
    }

    public Flux<Submission> findSubmissionsByUserForContest(String userId, String contestId, Pageable p) {
        return submissionRepository.findByUserIdAndContestId(userId, contestId);
    }

    public Mono<Long> countSubmissionsByUserForContest(String userId, String contestId) {
        return submissionRepository.countByUserIdAndContestId(userId, contestId);
    }

    public Flux<Submission> findSubmissionsByUserForProblemAndContest(String userId, String problemId, String contestId, Pageable p) {
        return submissionRepository.findByUserIdAndContestIdAndProblemId(userId, contestId, problemId, p);
    }

    public Mono<Submission> saveSubmission(Submission submission) {
        return submissionRepository.save(submission);
    }

    public Mono<QueuedSubmission> createSubmissionAndJudge(Problem problem, User user, Contest contest, String language, String code) {
        Submission sub = new Submission();
        sub.setId(ObjectId.get().toString());
        sub.setLang(language);
        sub.setUserId(user.getId());
        sub.setProblemId(problem.getId());
        sub.setCode(code);
        sub.setTimeSubmitted(System.currentTimeMillis());
        sub.setJudgingCompleted(false);
        sub.setPoints(0);
        sub.setMaxPoints(problem.getBatchPointsSum());
        sub.setStatus(Submission.SubmissionStatus.AWAITING_RUNNER);
        if (contest != null) sub.setContestId(contest.getId());
        sub.setBatchCases(problem.getSubmissionBatchCases());

        return saveSubmission(sub)
                .doOnNext(s -> submissionWebSocketService.sendLiveSubmissionListEntry(s).subscribe())
                .flatMap(s -> queuedSubmissionService.saveQueuedSubmission(new QueuedSubmission(s.getId(), problem.getId())))
                .doOnNext(q -> queuedSubmissionService.checkRunnersTask())
                .flatMap(Mono::just);
    }
}
