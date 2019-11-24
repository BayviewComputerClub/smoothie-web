package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.ProblemRepository;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.SubmissionRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class SmoothieSubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private ProblemRepository problemRepository;

    public Mono<Submission> findSubmissionById(String id) {
        return submissionRepository.findById(id);
    }

}
