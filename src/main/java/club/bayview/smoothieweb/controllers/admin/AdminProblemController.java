package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.JudgeLanguage;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.TestData;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
public class AdminProblemController {

    @Autowired
    SmoothieProblemService problemService;

    // -=-=-=-=-=-=-=-=-=-=- Schema -=-=-=-=-=-=-=-=-=-=-

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemFormLimit {
        private String lang;
        private double timeLimit, memoryLimit; // time limit in seconds, memory limit in mb
    }

    @Getter
    @Setter
    public static class ProblemForm {
        @NotNull
        @Size(min = 2)
        private String name,
                prettyName;

        @NotNull
        private String problemStatement;

        @NotNull
        private boolean allowPartial;

        @NotNull
        @Min(0)
        private int totalScoreWorth;

        @NotNull
        private List<ProblemFormLimit> limits;

        private Problem toProblem(Problem original) {
            Problem problem = original == null ? new Problem() : original;

            problem.setName(this.getName());
            problem.setPrettyName(this.getPrettyName());
            problem.setProblemStatement(this.getProblemStatement());
            problem.setAllowPartial(this.isAllowPartial());
            problem.setTotalPointsWorth(this.getTotalScoreWorth());

            problem.setLimits(new ArrayList<>());
            for (ProblemFormLimit l : this.getLimits()) {
                problem.getLimits().add(new Problem.ProblemLimits(JudgeLanguage.prettyToName(l.getLang()), l.timeLimit, l.memoryLimit));
            }

            return problem;
        }
    }

    private static ProblemForm defaultProblem;

    static {
        defaultProblem = new ProblemForm();
        defaultProblem.name = "";
        defaultProblem.prettyName = "";
        defaultProblem.allowPartial = false;
        defaultProblem.totalScoreWorth = 1;
        defaultProblem.limits = Arrays.asList(new ProblemFormLimit(JudgeLanguage.ALL.getPrettyName(), 1.0, 256));
        defaultProblem.problemStatement = "This is the problem statement.\n" +
                "<br/>\n" +
                "Please solve $$a + b = c$$.\n" +
                "\n" +
                "## Input Specification\n" +
                "The first line of input contains the integers $$a$$ and $$b$$ which are the integers to add.\n" +
                "<br/>\n" +
                "<br/>\n" +
                "$$1 \\le a, b \\le 10$$\n" +
                "\n" +
                "## Output Specification\n" +
                "The output contains $$c$$. which is the sum of $$a + b$$.\n" +
                "\n" +
                "## Sample Input\n" +
                "```\n" +
                "1 1\n" +
                "```\n" +
                "\n" +
                "## Sample Output\n" +
                "```\n" +
                "2\n" +
                "```\n";
    }

    // -=-=-=-=-=-=-=-=-=-=- Routes -=-=-=-=-=-=-=-=-=-=-

    @GetMapping("/admin/new-problem")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getNewProblemRoute(Model model) {
        model.addAttribute("newProblem", true);
        model.addAttribute("problem", defaultProblem);
        model.addAttribute("languages", JudgeLanguage.values);
        return Mono.just("admin/edit-problem");
    }

    @GetMapping("/problem/{name}/edit")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getEditProblemRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return problemService.findProblemByName(name).flatMap(p -> {
            if (p == null) return Mono.just("404");

            model.addAttribute("newProblem", false);
            model.addAttribute("problem", problemToProblemForm(p));
            model.addAttribute("languages", JudgeLanguage.values);
            return Mono.just("admin/edit-problem");
        });
    }

    @GetMapping("/problem/{name}/manage")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getManageProblemRoute(@PathVariable String name, Model model) {
        if (name == null) return Mono.just("404");

        return problemService.findProblemByName(name).flatMap(p -> {
            if (p == null) return Mono.just("404");

            model.addAttribute("problem", p);

            return Mono.just("admin/manage-problem");
        });
    }

    @PostMapping("/admin/new-problem")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postNewProblemRoute(@Valid ProblemForm form, BindingResult result, Model model) {

        if (result.hasErrors()) { // TODO
            model.addAttribute("newProblem", true);
            model.addAttribute("problem", form);
            model.addAttribute("languages", JudgeLanguage.values);
            return Mono.just("admin/edit-problem");
        } else {
            Problem p = form.toProblem(null);
            p.setTimeCreated(System.currentTimeMillis() / 1000L);

            return problemService.saveProblem(p)
                    .then(Mono.just("redirect:/problem/" + p.getName() + "/manage"));
        }
    }

    @PostMapping("/problem/{name}/edit")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postEditProblemRoute(@PathVariable String name, @Valid ProblemForm form, BindingResult result, Model model) throws IOException {
        if (result.hasErrors()) { // TODO form errors
            model.addAttribute("newProblem", false);
            model.addAttribute("problem", form);
            model.addAttribute("languages", JudgeLanguage.values);
            return Mono.just("admin/edit-problem");
        } else {
            // save problem
            return problemService.findProblemByName(name).flatMap(originalProblem -> {
                if (originalProblem == null) return Mono.just("404");

                return problemService.saveProblem(form.toProblem(originalProblem))
                        .then(Mono.just("redirect:/problem/" + name));
            });

        }
    }

    private ProblemForm problemToProblemForm(Problem p) {
        ProblemForm pf = new ProblemForm();
        pf.setName(p.getName());
        pf.setPrettyName(p.getPrettyName());
        pf.setProblemStatement(p.getProblemStatement());
        pf.setAllowPartial(p.isAllowPartial());
        pf.setTotalScoreWorth(p.getTotalPointsWorth());
        pf.setLimits(new ArrayList<>());
        for (Problem.ProblemLimits l : p.getLimits()) {
            pf.getLimits().add(new ProblemFormLimit(JudgeLanguage.nameToPretty(l.getLang()), l.getTimeLimit(), l.getMemoryLimit()));
        }
        return pf;
    }

}
