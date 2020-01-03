package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.controllers.LiveSubmissionController;
import club.bayview.smoothieweb.models.JudgeLanguage;
import club.bayview.smoothieweb.models.Runner;
import club.bayview.smoothieweb.models.RunnerRepository;
import club.bayview.smoothieweb.models.Submission;
import com.google.common.collect.Iterables;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Service
public class SmoothieRunnerService implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private RunnerRepository runnerRepository;

    @Autowired
    private SmoothieSubmissionService submissionService;

    @Autowired
    private LiveSubmissionController liveSubmissionController;

    @Autowired
    private SmoothieUserService userService;

    @Autowired
    private SmoothieProblemService problemService;

    private HashMap<String, SmoothieRunner> runners = new HashMap<>();

    // -=-=-=-=-=- CRUD -=-=-=-=-=-

    public Mono<Runner> findRunnerByName(String name) {
        return runnerRepository.findByName(name);
    }

    public Mono<Runner> findRunnerById(String id) {
        return runnerRepository.findById(id);
    }

    public Flux<Runner> findAllRunners() {
        return runnerRepository.findAll();
    }

    public Mono<Void> saveRunner(Runner runner) {
        return runnerRepository.save(runner).then();
    }

    public SmoothieRunner getSmoothieRunner(String id) {
        return runners.get(id);
    }

    public void updateSmoothieRunner(Runner runner) {
        if (runners.get(runner.getId()) != null) {
            // cleanly stop old smoothie runner, and allow existing tasks to finish
            runners.get(runner.getId()).cleanStop();
        }
        runners.put(runner.getId(), new SmoothieRunner(runner));
    }

    // -=-=-=-=-=- Functional -=-=-=-=-=-

    @Override
    public void onApplicationEvent(ContextRefreshedEvent e) {
        // initialize runners
        findAllRunners().subscribe(runner -> runners.put(runner.getId(), new SmoothieRunner(runner)));
    }

    /**
     * Run a full grader session asynchronously
     */

    public void grade(club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest req, Submission submission) {
        SmoothieRunner runner = getAvailableRunner(JudgeLanguage.valueOf(req.getSolution().getLanguage()));

        submission.setRunnerId(findRunnerById(runner.getId()).block().getId());
        submissionService.saveSubmission(submission).subscribe();

        StreamObserver<club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest> observer = runner.getAsyncStub().testSolution(new GraderStreamObserver(submission));

        // send request
        observer.onNext(req);
        observer.onCompleted();
    }

    /**
     * Get an available runner for judging.
     *
     * @param language the programming language needed
     * @return the runner, or null if none are available
     */
    public SmoothieRunner getAvailableRunner(JudgeLanguage language) {

        return Iterables.get(runners.values(), 0); // TODO this is temporary
    }

}
