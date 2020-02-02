package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.QueuedSubmission;
import club.bayview.smoothieweb.models.QueuedSubmissionRepository;
import club.bayview.smoothieweb.models.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * Find tasks for runners to work on.
     */

    @Async
    public synchronized void checkRunnersTask() {
        var runners = new ArrayList<>(runnerService.getSmoothieRunners().values());
        if (runners.size() == 0) return; // if there are no runners, return

        logger.debug("Running checkRunnersTask, and searching for runners...");

        getQueuedSubmissions().subscribe(sub -> {
            var smoothieRunners = sortRunnersForSubmission(runners, sub);

            logger.debug("Looking at submission " + sub.getSubmissionId() + " for runners..");
            for (SmoothieRunner runner : smoothieRunners) {
                // grade if there are no tasks in the queue
                if (runner.getHealth().getNumOfTasksInQueue() == 0) {

                    AtomicReference<Submission> s = new AtomicReference<>();
                    deleteQueuedSubmissionById(sub.getId())
                            .flatMap(t -> Mono.zip(problemService.findProblemById(sub.getProblemId()), submissionService.findSubmissionById(sub.getSubmissionId())))
                            .flatMap(t -> {
                                s.set(t.getT2());
                                return toTestSolutionRequest(t.getT1(), t.getT2());
                            })
                            .flatMap(tsr -> {
                                runner.grade(tsr, s.get());
                                return submissionService.saveSubmission(s.get()); // the runner id was set
                            }).subscribe();

                    break; // leave when finished
                }
            }
        });
    }

    // create grpc object to send
    private Mono<club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest> toTestSolutionRequest(Problem p, Submission s) {
        return p.getGRPCObject(s.getLang()).flatMap(grpcProblem -> Mono.just(club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest.newBuilder()
                .setTestBatchEvenIfFailed(false)
                .setCancelTesting(false)
                .setProblem(grpcProblem)
                .setSolution(club.bayview.smoothieweb.SmoothieRunner.Solution.newBuilder()
                        .setCode(s.getCode())
                        .setLanguage(s.getLang())
                        .build())
                .build()));
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
