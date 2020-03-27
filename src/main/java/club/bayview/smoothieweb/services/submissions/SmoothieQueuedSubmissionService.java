package club.bayview.smoothieweb.services.submissions;

import club.bayview.smoothieweb.models.QueuedSubmission;
import club.bayview.smoothieweb.repositories.QueuedSubmissionRepository;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import io.grpc.ConnectivityState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    @Autowired
    RunnerTaskContextProcessorService runnerTaskService;

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
     * TODO better syncing
     */

    @Async
    public synchronized void checkRunnersTask() {
        var runners = new ArrayList<>(runnerService.getSmoothieRunners().values());
        if (runners.size() == 0) return; // if there are no runners, return

        logger.debug("Running checkRunnersTask, and searching for runners...");

        for (var sub : getQueuedSubmissions().collectList().block()) {
            var smoothieRunners = sortRunnersForSubmission(runners, sub);

            logger.debug("Looking at submission " + sub.getSubmissionId() + " for runners..");
            for (SmoothieRunner runner : smoothieRunners) {
                // grade if there are no tasks in the queue
                if (!runner.isOccupied() && runner.getHealth().getNumOfTasksInQueue() == 0) {
                    deleteQueuedSubmissionById(sub.getId())
                            .doOnNext(t -> runnerTaskService.addTask(runner.getId(), RunnerTaskProcessorEvent.builder()
                                    .eventType(RunnerTaskProcessorEvent.EventType.JUDGE_SUBMISSION)
                                    .queuedSubmission(sub)
                                    .build())).block();
                    break; // leave when finished
                }
            }
        }
    }

    public ArrayList<SmoothieRunner> sortRunnersForSubmission(ArrayList<SmoothieRunner> runners, QueuedSubmission submission) {
        ArrayList<SmoothieRunner> order = new ArrayList<>();
        if (submission.getRequestedRunnerIds() == null || submission.getRequestedRunnerIds().isEmpty()) {
            // if the submission has requested runners
            runners.stream()
                    .filter(runner -> runner.getStatus().equals(ConnectivityState.IDLE) || runner.getStatus().equals(ConnectivityState.READY))
                    .forEach(order::add);
        } else {
            // if the submission allows for all runners
            for (var runner : runners) {
                if (runner.getStatus().equals(ConnectivityState.IDLE) || runner.getStatus().equals(ConnectivityState.READY)) {
                    if (submission.getRequestedRunnerIds().contains(runner.getId())) {
                        order.add(runner);
                    }
                }
            }
        }
        order.sort(Collections.reverseOrder()); // descending in rank
        return order;
    }

}
