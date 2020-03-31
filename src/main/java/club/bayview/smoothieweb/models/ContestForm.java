package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieUserService;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
        defaultContest.setRenderedDescription("");
        defaultContest.setTimeStart(0);
        defaultContest.setTimeEnd(0);
        defaultContest.setSubmissionPeriod(0);
        defaultContest.setEnabled(false);
        defaultContest.setVisibleToPublic(false);
        defaultContest.setTimeMatters(false);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContestProblemForm {
        private int contestProblemNumber,
                totalPointsWorth;

        private String problemName,
                customName,
                colourHex;

        SmoothieProblemService problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);

        public ContestProblemForm(Contest.ContestProblem cp) {
            this.contestProblemNumber = cp.getContestProblemNumber();
            this.totalPointsWorth = cp.getTotalPointsWorth();
            Problem p = problemService.findProblemById(cp.getProblemId()).block();
            if (p != null) this.problemName = p.getName(); // TODO
            this.customName = cp.getCustomName();
            this.colourHex = cp.getColourHex();
        }

        public Contest.ContestProblem getContestProblem() {
            Contest.ContestProblem cp = new Contest.ContestProblem();
            cp.setContestProblemNumber(contestProblemNumber);
            cp.setTotalPointsWorth(totalPointsWorth);
            Problem p = problemService.findProblemByName(problemName).block();
            if (p != null) cp.setProblemId(p.getId()); // TODO
            cp.setCustomName(customName);
            cp.setColourHex(colourHex);
            return cp;
        }
    }

    SmoothieUserService userService = SmoothieWebApplication.context.getBean(SmoothieUserService.class);
    SmoothieProblemService problemService = SmoothieWebApplication.context.getBean(SmoothieProblemService.class);

    // fields
    @NotBlank
    @Size(min = 2)
    private String name,
            prettyName;

    private String description, renderedDescription;

    @NotNull
    private List<ContestProblemForm> problems = new ArrayList<>();

    @NotNull
    private List<String> testerUserHandles = new ArrayList<>(),
            editorUserHandles = new ArrayList<>(),
            juryUserHandles = new ArrayList<>();

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
        cf.setRenderedDescription(c.getRenderedDescription());

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
                .thenMany(userService.resolveIdsToHandles(c.getEditorUserIds()))
                .doOnNext(cf.getEditorUserHandles()::add)
                .thenMany(userService.resolveIdsToHandles(c.getJuryUserIds()))
                .doOnNext(cf.getJuryUserHandles()::add)
                .then(Mono.just(cf));
    }

    public Mono<Contest> toContest(Contest c) {
        if (c == null) {
            c = new Contest();
        }

        c.setName(name);
        c.setPrettyName(prettyName);
        c.setDescription(description);
        c.setRenderedDescription(renderedDescription);

        if (c.getTimeStart() != timeStart || c.getTimeEnd() != timeEnd || c.getSubmissionPeriod() != submissionPeriod) {
            // TODO update everyone's timers
        }
        c.setTimeStart(timeStart);
        c.setTimeEnd(timeEnd);
        c.setSubmissionPeriod(submissionPeriod);
        if (c.getTimeCreated() == 0) c.setTimeCreated(System.currentTimeMillis());

        c.setEnabled(enabled);
        c.setVisibleToPublic(visibleToPublic);
        c.setTimeMatters(timeMatters);

        c.getContestProblems().clear();
        for (var p : problems) {
            Contest.ContestProblem cp = p.getContestProblem();
            c.getContestProblems().put(cp.getProblemId(), cp);
        }

        // resolve handle -> id for testers and editors list
        c.getTesterUserIds().clear();
        c.getEditorUserIds().clear();
        c.getJuryUserIds().clear();

        // redo scoreboard
        c.updateLeaderBoard();

        return userService.resolveHandlesToIds(testerUserHandles)
                .doOnNext(c.getTesterUserIds()::add)
                .thenMany(userService.resolveHandlesToIds(editorUserHandles))
                .doOnNext(c.getEditorUserIds()::add)
                .thenMany(userService.resolveHandlesToIds(juryUserHandles))
                .doOnNext(c.getJuryUserIds()::add)
                .then(Mono.just(c));
    }

}
