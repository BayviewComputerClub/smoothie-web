package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SmoothieSubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    public Mono<Submission> findSubmissionById(String id) {
        return submissionRepository.findById(id);
    }

}
