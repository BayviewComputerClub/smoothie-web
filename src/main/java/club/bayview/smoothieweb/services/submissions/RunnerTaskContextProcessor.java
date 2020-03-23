package club.bayview.smoothieweb.services.submissions;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.QueuedSubmission;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

public class RunnerTaskContextProcessor implements Runnable {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    SmoothieSubmissionService submissionService = SmoothieWebApplication.context.getBean(SmoothieSubmissionService.class);
    SmoothieQueuedSubmissionService queuedSubmissionService = SmoothieWebApplication.context.getBean(SmoothieQueuedSubmissionService.class);
    SmoothieProblemService problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);

    UnicastProcessor<RunnerTaskProcessorEvent> taskQueue = UnicastProcessor.create();
    String currentSubmissionId, currentQueuedSubmissionId;
    SmoothieRunner runner;

    public RunnerTaskContextProcessor(SmoothieRunner runner) {
        this.runner = runner;
    }

    // todo flag smoothierunner as "occupied" when judging
    // assumes that judge_submission would not be sent if a submission is ongoing

    @Override
    public void run() {
        logger.info("Started worker thread for runner {}.", runner.getName());

        taskQueue.subscribe(this::processEvent);
    }

    public Mono<Void> processEvent(RunnerTaskProcessorEvent ev) {
        switch (ev.eventType) {
            case RUNNER_GRADER_RECV_MSG:
                break;
            case RUNNER_GRADER_COMPLETE:
                break;
            case RUNNER_GRADER_ERR:
                break;
            case RUNNER_UPLOAD_RECV_MSG:
                break;
            case RUNNER_UPLOAD_COMPLETE:
                break;
            case RUNNER_UPLOAD_ERR:
                break;
            case RUNNER_TRANSIENT_FAILURE:
                break;
            case CANCEL_SUBMISSION:
                return cancelSubmission();
            case JUDGE_SUBMISSION:
                return judgeSubmission(ev.getQueuedSubmission());
            case STOP:
                return Mono.error(new Exception());
        }
        return Mono.empty();
    }

    public Mono<Void> judgeSubmission(QueuedSubmission qs) {
        currentQueuedSubmissionId = qs.getId();
        currentSubmissionId = qs.getSubmissionId();

        return submissionService.findSubmissionById(currentSubmissionId)
                .doOnNext(s -> {
                    s.setRunnerId(runner.getId());
                    s.setStatus(Submission.SubmissionStatus.JUDGING);
                })
                .flatMap(s -> Mono.zip(submissionService.saveSubmission(s), problemService.findProblemById(s.getProblemId())))
                .flatMap(t -> Mono.zip(Mono.just(t.getT1()), toTestSolutionRequest(t.getT2(), t.getT1())))
                .doOnNext(t -> runner.grade(t.getT2(), t.getT1()))
                .then();
    }

    public Mono<Void> cancelSubmission() {

    }

    // create grpc object to send
    private Mono<club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest> toTestSolutionRequest(Problem p, Submission s) {
        return p.getGRPCObject(s.getLang())
                .flatMap(grpcProblem -> Mono.just(club.bayview.smoothieweb.SmoothieRunner.TestSolutionRequest.newBuilder()
                        .setTestBatchEvenIfFailed(false)
                        .setCancelTesting(false)
                        .setProblem(grpcProblem)
                        .setSolution(club.bayview.smoothieweb.SmoothieRunner.Solution.newBuilder()
                                .setCode(s.getCode() == null ? "": s.getCode())
                                .setLanguage(s.getLang() == null ? "" : s.getLang())
                                .build())
                        .build()));
    }
}
