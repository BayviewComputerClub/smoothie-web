package club.bayview.smoothieweb.services.submissions;

import club.bayview.smoothieweb.services.SmoothieRunner;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class RunnerTaskContextProcessorService {
    // runner id -> context processor
    HashMap<String, RunnerTaskContextProcessor> contextProcessorHashMap = new HashMap<>();

    public void addTask(String runnerId, RunnerTaskProcessorEvent ev) {
        // todo null check
        contextProcessorHashMap.get(runnerId).taskQueue.onNext(ev);
    }

    // TODO
    public void initContextProcessorForRunner(SmoothieRunner runner) {
        contextProcessorHashMap.put(runner.getId(), new RunnerTaskContextProcessor(runner));
    }
}
