package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.RunnerRepository;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.SubmissionRepository;
import com.google.common.collect.Iterables;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class SmoothieRunnerService implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private RunnerRepository runnerRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    private Logger logger = LoggerFactory.getLogger(SmoothieRunner.class);

    private HashMap<String, SmoothieRunner> runners = new HashMap<>();

    @Override
    public void onApplicationEvent(ContextRefreshedEvent e) {
        // initialize runners
        runnerRepository.findAll().subscribe(runner -> runners.put(runner.getId(), new SmoothieRunner(runner.getId(), runner.getHost(), runner.getPort())));
    }

    /**
     * Run a full grader session asynchronously
     */
    public void grade(club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest req, Submission submission) {
        SmoothieRunner runner = getAvailableRunner(req.getSolution().getLanguage());

        StreamObserver<club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest> observer = runner.getAsyncStub().testSolution(new StreamObserver<>() {

            @Override
            public void onNext(club.bayview.smoothieweb.SmoothieRunner.TestSolutionResponse value) {

                if (!value.getCompileError().equals("")) { // compile error
                    submission.setCompileError(value.getCompileError());
                } else if (value.getCompletedTesting()) { // testing has completed
                    submission.setJudgingCompleted(true);
                } else {
                    for (Submission.SubmissionBatchCase c : submission.getBatchCases()) {

                        if (c.getBatchNumber() == value.getTestCaseResult().getBatchNumber() && c.getCaseNumber() == value.getTestCaseResult().getCaseNumber()) {
                            c.setError(value.getTestCaseResult().getResultInfo());
                            c.setMemUsage(value.getTestCaseResult().getMemUsage());
                            c.setTime(value.getTestCaseResult().getTime());
                            c.setResultCode(value.getTestCaseResult().getResult());
                            // TODO send to websocket
                        }

                    }
                }

                submissionRepository.save(submission);
            }

            @Override
            public void onError(Throwable t) {
                logger.error(t.toString());
            }

            @Override
            public void onCompleted() {
                logger.info("Judging has completed for submission " + submission.getId() + ".");
            }
        });

        // send request
        observer.onNext(req);
    }

    /**
     * Get an available runner for judging.
     * @param language the programming language needed
     * @return the runner, or null if none are available
     */
    public SmoothieRunner getAvailableRunner(String language) {
        return Iterables.get(runners.values(), 0); // TODO this is temporary
    }

}
