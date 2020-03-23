package club.bayview.smoothieweb.services.submissions;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.models.QueuedSubmission;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RunnerTaskProcessorEvent {

    public enum EventType {
        RUNNER_GRADER_RECV_MSG,             // received TestSolutionResponse grpc message from TestSolution()
        RUNNER_GRADER_COMPLETE,             // onComplete message from grpc TestSolution()
        RUNNER_GRADER_ERR,                  // error message from grpc TestSolution()
        RUNNER_UPLOAD_RECV_MSG,             // received message from grpc UploadProblemTestData()
        RUNNER_UPLOAD_COMPLETE,             // onComplete message from grpc UploadProblemTestData()
        RUNNER_UPLOAD_ERR,                  // error message from grpc UploadProblemTestData()
        RUNNER_TRANSIENT_FAILURE,           // runner has failed
        CANCEL_SUBMISSION,                  // request to cancel current submission judging
        JUDGE_SUBMISSION,                   // request to judge a submission
        STOP,                               // runner has been deleted
    }

    EventType eventType;
    Throwable error;
    SmoothieRunner.TestSolutionResponse testSolutionResponse;
    SmoothieRunner.UploadTestDataResponse uploadTestDataResponse;
    QueuedSubmission queuedSubmission;
}
