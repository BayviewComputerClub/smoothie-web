package club.bayview.smoothieweb.services.submissions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class RunnerTaskContextProcessorService {

    @Autowired
    SmoothieRunnerService runnerService;

    // runner id -> context processor
    HashMap<String, RunnerTaskContextProcessor> contextProcessorHashMap = new HashMap<>();

    public void addTask(String runnerId, RunnerTaskProcessorEvent ev) {
        // add runner context processor if it isn't there
        if (!contextProcessorHashMap.containsKey(runnerId)) {
            if (runnerService.getSmoothieRunners().containsKey(runnerId)) {
                initContextProcessorForRunner(runnerService.getSmoothieRunner(runnerId));
            } else {
                return;
            }
        }
        // add to task queue
        contextProcessorHashMap.get(runnerId).taskQueue.onNext(ev);
    }

    public void initContextProcessorForRunner(SmoothieRunner runner) {
        var rtcp = new RunnerTaskContextProcessor(runner);
        contextProcessorHashMap.put(runner.getId(), rtcp);
        rtcp.run();
    }
}
