package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.QueuedSubmission;
import club.bayview.smoothieweb.models.QueuedSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;

@Service
public class SmoothieQueuedSubmissionService {

    private Logger logger = LoggerFactory.getLogger(SmoothieQueuedSubmissionService.class);

    @Autowired
    QueuedSubmissionRepository queuedSubmissionRepository;

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieSubmissionService submissionService;

    @Autowired
    SmoothieRunnerService runnerService;

    public Mono<QueuedSubmission> saveQueuedSubmission(QueuedSubmission q) {
        return queuedSubmissionRepository.save(q);
    }

    public Flux<QueuedSubmission> getQueuedSubmissions() {
        return queuedSubmissionRepository.findAllByOrderByTimeRequestedAsc();
    }

    public Mono<Long> deleteQueuedSubmissionById(String id) {
        return queuedSubmissionRepository.deleteAllById(id);
    }

    @Async
    public synchronized void checkRunnersTask() {
        var runners = new ArrayList<>(runnerService.getSmoothieRunners().values());
        if (runners.size() == 0) return; // if there are no runners, return

        logger.debug("Running checkRunnersTask, and searching for runners...");

        getQueuedSubmissions().subscribe(sub -> {
            var smoothieRunners = sortRunnersForSubmission(runners, sub);
            var runner = smoothieRunners.get(0);

            logger.debug("Looking at submission " + sub.getSubmissionId() + " for runners..");

            // grade if there are no tasks in the queue
            if (runner.getHealth().getNumOfTasksInQueue() == 0) {
                deleteQueuedSubmissionById(sub.getId())
                        .flatMap(t -> Mono.zip(problemService.findProblemById(sub.getProblemId()), submissionService.findSubmissionById(sub.getSubmissionId())))
                        .subscribe(s -> {
                            // create grpc object to send
                            club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest req = club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest.newBuilder()
                                    .setTestBatchEvenIfFailed(false)
                                    .setCancelTesting(false)
                                    .setSolution(club.bayview.smoothieweb.SmoothieRunner.Solution.newBuilder()
                                            .setCode(s.getT2().getCode())
                                            .setLanguage(s.getT2().getLang())
                                            .setProblem(s.getT1().getGRPCObject(s.getT2().getLang()))
                                            .build())
                                    .build();

                            runner.grade(req, s.getT2());
                        });
            }
        });
    }

    public ArrayList<SmoothieRunner> sortRunnersForSubmission(ArrayList<SmoothieRunner> runners, QueuedSubmission submission) {
        ArrayList<SmoothieRunner> order = new ArrayList<>();
        if (submission.getRequestedRunnerIds() == null || submission.getRequestedRunnerIds().isEmpty()) {
            order = runners;
        } else {
            for (var runner : runners) {
                if (submission.getRequestedRunnerIds().contains(runner.getId())) {
                    order.add(runner);
                }
            }
        }
        order.sort(Collections.reverseOrder()); // descending in rank
        return order;
    }

}
