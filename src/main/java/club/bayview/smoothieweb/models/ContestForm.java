package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.SmoothieUserService;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class ContestForm {

    public static ContestForm defaultContest = new ContestForm();

    static {
        defaultContest.setName("");
        defaultContest.setPrettyName("");
        defaultContest.setDescription("");
        defaultContest.setTimeStart(0);
        defaultContest.setTimeEnd(0);
        defaultContest.setSubmissionPeriod(0);
        defaultContest.setEnabled(false);
        defaultContest.setVisibleToPublic(false);
        defaultContest.setTimeMatters(false);
    }

    @Getter
    @Setter
    public static class ContestProblemForm {
        private int contestProblemNumber,
                totalPointsWorth;

        private String problemId,
                customName,
                colourHex;

        public ContestProblemForm(Contest.ContestProblem cp) {
            this.contestProblemNumber = cp.getContestProblemNumber();
            this.totalPointsWorth = cp.getTotalPointsWorth();
            this.problemId = cp.getProblemId();
            this.customName = cp.getCustomName();
            this.colourHex = cp.getColourHex();
        }

        public Contest.ContestProblem getContestProblem() {
            Contest.ContestProblem cp = new Contest.ContestProblem();
            cp.setContestProblemNumber(contestProblemNumber);
            cp.setTotalPointsWorth(totalPointsWorth);
            cp.setProblemId(problemId);
            cp.setCustomName(customName);
            cp.setColourHex(colourHex);
            return cp;
        }
    }

    SmoothieUserService userService = SmoothieWebApplication.context.getBean(SmoothieUserService.class);

    // fields
    @NotBlank
    @Size(min = 2)
    private String name,
            prettyName;

    private String description;

    @NotNull
    private List<ContestProblemForm> problems = new ArrayList<>();

    @NotNull
    private List<String> testerUserHandles = new ArrayList<>(),
            editorUserHandles = new ArrayList<>();

    @Min(0)
    private long timeStart,
            timeEnd,
            submissionPeriod;

    private boolean enabled,
            visibleToPublic,
            timeMatters;

    // adapters
    public static Mono<ContestForm> fromContest(Contest c) {
        ContestForm cf = new ContestForm();
        cf.setName(c.getName());
        cf.setPrettyName(c.getPrettyName());
        cf.setDescription(c.getDescription());

        cf.setTimeStart(c.getTimeStart());
        cf.setTimeEnd(c.getTimeEnd());
        cf.setSubmissionPeriod(c.getSubmissionPeriod());

        cf.setEnabled(c.isEnabled());
        cf.setVisibleToPublic(c.isVisibleToPublic());
        cf.setTimeMatters(c.isTimeMatters());

        c.getContestProblemsInOrder().forEach(p -> cf.problems.add(new ContestProblemForm(p)));

        SmoothieUserService userService = SmoothieWebApplication.context.getBean(SmoothieUserService.class);

        return userService
                .resolveIdsToHandles(c.getTesterUserIds())
                .doOnNext(cf.getTesterUserHandles()::add)
                .thenMany(SmoothieWebApplication.context.getBean(SmoothieUserService.class).resolveHandlesToIds(c.getEditorUserIds()))
                .doOnNext(cf.getEditorUserHandles()::add)
                .then(Mono.just(cf));
    }

    public Mono<Contest> toContest(Contest c) {
        if (c == null) {
            c = new Contest();
        }

        c.setName(name);
        c.setPrettyName(prettyName);
        c.setDescription(description);

        if (c.getTimeStart() != timeStart || c.getTimeEnd() != timeEnd || c.getSubmissionPeriod() != submissionPeriod) {
            // TODO update everyone's timers
        }
        c.setTimeStart(timeStart);
        c.setTimeEnd(timeEnd);
        c.setSubmissionPeriod(submissionPeriod);

        c.setEnabled(enabled);
        c.setVisibleToPublic(visibleToPublic);
        c.setTimeMatters(timeMatters);

        for (var p : problems) {
            c.getContestProblems().put(p.getProblemId(), p.getContestProblem());
        }

        // resolve handle -> id for testers and editors list
        c.getTesterUserIds().clear();
        c.getEditorUserIds().clear();

        // redo scoreboard
        c.updateLeaderBoard();

        return userService.resolveHandlesToIds(testerUserHandles)
                .doOnNext(c.getTesterUserIds()::add)
                .thenMany(userService.resolveHandlesToIds(editorUserHandles))
                .doOnNext(c.getEditorUserIds()::add)
                .then(Mono.just(c));
    }

}
