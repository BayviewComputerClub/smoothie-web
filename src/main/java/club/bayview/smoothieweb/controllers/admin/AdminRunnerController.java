package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.Runner;
import club.bayview.smoothieweb.services.SmoothieRunnerService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NotFoundException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Controller
public class AdminRunnerController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieRunnerService runnerService;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RunnerForm {
        @NotNull
        @Size(min = 2)
        private String name;

        @NotNull
        private String description;

        @NotNull
        @Size(min = 2)
        private String host;

        @NotNull
        @Min(1)
        @Max(65535)
        private int port;
    }

    private static RunnerForm defaultRunner = new RunnerForm();

    static {
        defaultRunner.setName("");
        defaultRunner.setDescription("");
        defaultRunner.setHost("");
        defaultRunner.setPort(6821);
    }

    @GetMapping("/admin/runners")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getRunnersRoute(Model model) {
        return runnerService.findAllRunners().collectList().flatMap(runners -> {
            model.addAttribute("runners", runners);
            return Mono.just("admin/runners");
        });
    }

    @GetMapping("/admin/runner/{name}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getRunnerRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return runnerService.findRunnerByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(runner -> {
                    model.addAttribute("runner", runner);
                    return Mono.just("admin/runner");
                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /admin/runner/{name} route exception: "));
    }

    @GetMapping("/admin/new-runner")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String getNewRunnerRoute(Model model) {
        model.addAttribute("newRunner", true);
        model.addAttribute("runner", defaultRunner);
        return "admin/runner-edit";
    }

    @GetMapping("/admin/runner/{name}/edit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getEditRunnerRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return runnerService.findRunnerByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(runner -> {
                    if (runner == null) return Mono.just("404");

                    model.addAttribute("newRunner", false);
                    model.addAttribute("runner", runnerToRunnerForm(runner));
                    return Mono.just("admin/runner-edit");
                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /admin/runner/{name}/edit route exception: "));
    }

    @PostMapping("/admin/new-runner")
    @PreAuthorize(("hasRole('ROLE_ADMIN')"))
    public Mono<String> postNewRunnerRoute(@Valid RunnerForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("newRunner", true);
            model.addAttribute("runner", form);
            return Mono.just("admin/runner-edit");
        }
        // TODO check if name exists

        Runner r = runnerFormToRunner(null, form);
        r.setId(ObjectId.get().toString());

        runnerService.updateSmoothieRunner(r);
        return runnerService.saveRunner(r).then(Mono.just("redirect:/admin/runners"));
    }

    @PostMapping("/admin/runner/{name}/edit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> postEditRunnerRoute(@Valid RunnerForm form, BindingResult result, @PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");
        if (result.hasErrors()) {
            model.addAttribute("newRunner", false);
            model.addAttribute("runner", form);
            return Mono.just("admin/runner-edit");
        }

        return runnerService.findRunnerByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(runner -> {
                    Runner r = runnerFormToRunner(runner, form);
                    runnerService.updateSmoothieRunner(r);
                    return runnerService.saveRunner(r);
                })
                .flatMap(b -> Mono.just("redirect:/admin/runners"))
                .onErrorResume(e -> ErrorCommon.handle404(e, logger, "POST /admin/runner/{name}/edit route exception: "));
    }

    private Runner runnerFormToRunner(Runner runner, RunnerForm form) {
        if (runner == null) runner = new Runner();

        runner.setName(form.getName());
        runner.setDescription(form.getDescription());
        runner.setHost(form.getHost());
        runner.setPort(form.getPort());
        return runner;
    }

    private RunnerForm runnerToRunnerForm(Runner runner) {
        RunnerForm form = new RunnerForm();
        form.setName(runner.getName());
        form.setDescription(runner.getDescription());
        form.setHost(runner.getHost());
        form.setPort(runner.getPort());
        return form;
    }
}
