package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Runner;
import club.bayview.smoothieweb.repositories.RunnerRepository;
import club.bayview.smoothieweb.services.submissions.RunnerTaskContextProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;

@Service
public class SmoothieRunnerService implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    RunnerRepository runnerRepository;

    @Autowired
    RunnerTaskContextProcessorService taskService;

    private HashMap<String, SmoothieRunner> runners = new HashMap<>();

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

    // todo async
    public void updateSmoothieRunner(Runner runner) {
        SmoothieRunner newRunner = new SmoothieRunner(runner);
        if (runners.get(runner.getId()) != null) {
            // cleanly stop old smoothie runner, and allow existing tasks to finish
            runners.get(runner.getId()).cleanStop();
        } else {
            // if new runner, create worker
            taskService.initContextProcessorForRunner(newRunner);
        }
        runners.put(runner.getId(), newRunner);
    }

    public HashMap<String, SmoothieRunner> getSmoothieRunners() {
        return runners;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent e) {
        // initialize runners
        findAllRunners().subscribe(runner -> {
            SmoothieRunner smoothieRunner = new SmoothieRunner(runner);
            // create worker
            taskService.initContextProcessorForRunner(smoothieRunner);
            // add smoothie runner to map
            runners.put(runner.getId(), smoothieRunner);
        });
    }

}
