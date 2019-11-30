package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.JudgeLanguage;
import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;

@Controller
public class AdminProblemController {

    @Autowired
    SmoothieProblemService problemService;

    @Getter
    @Setter
    public static class ProblemForm {
        @NotNull
        private String name,
                prettyName,
                problemStatement;

        @NotNull
        private boolean allowPartial;

        @NotNull
        private int totalScoreWorth;

        @NotNull
        private Collection<Problem.ProblemLimits> limits;

        @NotNull
        private Collection<Problem.ProblemBatchCase> batchCases;
    }

    @GetMapping("/admin/new-problem")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getNewProblemRoute(Model model) {
        model.addAttribute("languages", JudgeLanguage.values());
        return Mono.just("admin/edit-problem");
    }

    @GetMapping("/problem/{name}/edit")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getEditProblemRoute(@PathVariable String name, Model model) {
        model.addAttribute("problem", problemToProblemForm(problemService.findProblemByName(name)));
        model.addAttribute("languages", JudgeLanguage.values());
        return Mono.just("admin/edit-problem");
    }

    @RequestMapping(value="/admin/new-problem", params={"addLimit"})
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> addLimit() {
        
    }

    @PostMapping("/admin/new-problem")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public ModelAndView postNewProblemRoute(@Valid ProblemForm form, BindingResult result) {
        ModelAndView page = new ModelAndView();

        if (result.hasErrors()) {
            page.setViewName("admin/new-problem");
        } else {
            problemService.saveProblem(problemFormToProblem(null, form));
            page.setViewName("redirect:/admin/problems");
        }

        return page;
    }

    private Mono<ProblemForm> problemToProblemForm(Mono<Problem> problem) {
        return problem.map(p -> {
            ProblemForm pf = new ProblemForm();
            pf.setName(p.getName());
            pf.setPrettyName(p.getPrettyName());
            pf.setProblemStatement(p.getProblemStatement());
            pf.setAllowPartial(p.isAllowPartial());
            pf.setTotalScoreWorth(p.getTotalScoreWorth());
            pf.setLimits(p.getLimits());
            pf.setBatchCases(p.getTestData());
            return pf;
        });
    }

    private Problem problemFormToProblem(String id, ProblemForm form) {
        Problem problem = new Problem();
        if (id != null) problem.setId(id);
        problem.setName(form.getName());
        problem.setPrettyName(form.getPrettyName());
        problem.setProblemStatement(form.getProblemStatement());
        problem.setAllowPartial(form.isAllowPartial());
        problem.setTotalScoreWorth(form.getTotalScoreWorth());

        problem.setLimits(form.getLimits());
        problem.setTestData(form.getBatchCases());

        problem.setSubmissions(new ArrayList<>());
        return problem;
    }

}
