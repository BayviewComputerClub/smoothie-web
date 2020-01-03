package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.ProblemRepository;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SmoothieSubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    public Mono<Submission> findSubmissionById(String id) {
        return submissionRepository.findById(id);
    }

    public Flux<Submission> findSubmissionsByProblem(String problemId) {
        return submissionRepository.findByProblemIdIsOrderByTimeSubmittedDesc(problemId);
    }

    public Flux<Submission> findSubmissionsByUser(String userId) {
        return submissionRepository.findByUserIdIsOrderByTimeSubmittedDesc(userId);
    }

    public Mono<Submission> saveSubmission(Submission submission) {
        return submissionRepository.save(submission);
    }

}
