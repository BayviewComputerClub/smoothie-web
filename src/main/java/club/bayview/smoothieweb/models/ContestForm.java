package club.bayview.smoothieweb.models;

import club.bayview.smoothieweb.SmoothieWebApplication;
import club.bayview.smoothieweb.services.SmoothieUserService;
import lombok.Getter;
import lombok.Setter;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ContestForm {

    @Getter
    @Setter
    public class ContestProblemForm {

    }

    SmoothieUserService userService = SmoothieWebApplication.context.getBean(SmoothieUserService.class);

    // fields
    @NotBlank
    @Size(min = 2)
    private String name,
            prettyName;

    @NotNull
    private List<String> testerUserHandles = new ArrayList<>(),
            editorUserHandles = new ArrayList<>();

    @NotBlank
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

        cf.setTimeStart(c.getTimeStart());
        cf.setTimeEnd(c.getTimeEnd());
        cf.setSubmissionPeriod(c.getSubmissionPeriod());

        cf.setEnabled(c.isEnabled());
        cf.setVisibleToPublic(c.isVisibleToPublic());
        cf.setTimeMatters(c.isTimeMatters());

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

        if (c.getTimeStart() != timeStart || c.getTimeEnd() != timeEnd || c.getSubmissionPeriod() != submissionPeriod) {
            // TODO update everyone's timers
        }
        c.setTimeStart(timeStart);
        c.setTimeEnd(timeEnd);
        c.setSubmissionPeriod(submissionPeriod);

        c.setEnabled(enabled);
        c.setVisibleToPublic(visibleToPublic);
        if (c.isTimeMatters() != timeMatters) {
            // redo scoreboard
            c.setTimeMatters(timeMatters);
            c.updateLeaderBoard();
        }

        // resolve handle -> id for testers and editors list
        c.getTesterUserIds().clear();
        c.getEditorUserIds().clear();

        return userService.resolveHandlesToIds(testerUserHandles)
                .doOnNext(c.getTesterUserIds()::add)
                .thenMany(userService.resolveHandlesToIds(editorUserHandles))
                .doOnNext(c.getEditorUserIds()::add)
                .then(Mono.just(c));
    }

}
