package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.RunnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class SmoothieRunnerService implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private RunnerRepository runnerRepository;

    private HashMap<String, SmoothieRunner> runners = new HashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent e) {
        // initialize runners
        runnerRepository.findAll().subscribe(runner -> runners.put(runner.getId(), new SmoothieRunner(runner.getId(), runner.getHost(), runner.getPort())));
    }

    

}
