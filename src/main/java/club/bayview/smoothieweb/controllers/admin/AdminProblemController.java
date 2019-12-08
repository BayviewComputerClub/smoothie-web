package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.SmoothieRunner;
import club.bayview.smoothieweb.models.JudgeLanguage;
import club.bayview.smoothieweb.models.Problem;
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
import org.springframework.web.servlet.ModelAndView;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
public class AdminProblemController {

    @Autowired
    SmoothieProblemService problemService;

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

        private MultipartFile testData;
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

    @GetMapping("/admin/new-problem")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getNewProblemRoute(Model model) {
        model.addAttribute("newProblem", true);
        model.addAttribute("problem", defaultProblem);
        model.addAttribute("languages", JudgeLanguage.values);
        return Mono.just("admin/problem");
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
            return Mono.just("admin/problem");
        });
    }

    @PostMapping("/admin/new-problem")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public ModelAndView postNewProblemRoute(@Valid ProblemForm form, BindingResult result) throws IOException {
        ModelAndView page = new ModelAndView();

        if (result.hasErrors()) { // TODO
            page.addObject("newProblem", true);
            page.addObject("problem", form);
            page.addObject("languages", JudgeLanguage.values);
            page.setViewName("admin/problem");
        } else {
            Problem p = problemFormToProblem(null, form);
            if (form.getTestData() != null) p.setTestData(getTestCasesFromZip(form.getTestData()));

            p.setTimeCreated(System.currentTimeMillis() / 1000L);

            problemService.saveProblem(p);
            page.setViewName("redirect:/admin/problems");
        }

        return page;
    }

    @PostMapping("/problem/{name}/edit")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postEditProblemRoute(@PathVariable String name, @Valid ProblemForm form, BindingResult result, Model model) throws IOException {
        if (result.hasErrors()) { // TODO form errors
            model.addAttribute("newProblem", false);
            model.addAttribute("problem", form);
            model.addAttribute("languages", JudgeLanguage.values);
            return Mono.just("admin/problem");
        } else {

            // save problem
            return problemService.findProblemByName(name).flatMap(originalProblem -> {
                if (originalProblem == null) return Mono.just("404");

                try {
                    Problem p = problemFormToProblem(originalProblem, form);
                    if (form.getTestData() != null) p.setTestData(getTestCasesFromZip(form.getTestData()));

                    problemService.saveProblem(p);
                    return Mono.just("redirect:/problem/" + name);
                } catch (Exception e) {
                    e.printStackTrace();
                    return Mono.just("500");
                }
            });

        }
    }

    private List<List<Problem.ProblemBatchCase>> getTestCasesFromZip(MultipartFile file) throws IOException {
        if (file == null) return new ArrayList<>();

        List<List<Problem.ProblemBatchCase>> list = new ArrayList<>();
        ZipInputStream zis = new ZipInputStream(file.getInputStream());
        ZipEntry entry;

        // TODO make this better with error checking and instruction file for points
        // format: 0-0.in, 0-0.out, 0-1.in, 0-1.out, etc...
        while ((entry = zis.getNextEntry()) != null) {

            // get vals
            String[] spl = entry.getName().split("-");
            int batchNum = Integer.parseInt(spl[0]), caseNum = Integer.parseInt(spl[1].split("\\.")[0]);
            boolean isInput = entry.getName().split("\\.")[1].equals("in");

            // get data
            StringBuilder data = new StringBuilder();
            Scanner scan = new Scanner(zis);
            while (scan.hasNextLine()) data.append(scan.nextLine());

            // add to list
            boolean found = false;

            if (batchNum >= list.size()) {
                for (int i = list.size(); i <= batchNum; i++) {
                    list.add(new ArrayList<>());
                }
            }

            for (var c : list.get(batchNum)) {
                if (c.getCaseNum() == caseNum) {
                    found = true;
                    if (isInput) {
                        c.setInput(data.toString());
                    } else {
                        c.setExpectedOutput(data.toString());
                    }
                }
            }

            if (!found) {
                if (isInput) list.get(batchNum).add(new Problem.ProblemBatchCase(batchNum, caseNum, data.toString(), ""));
                else list.get(batchNum).add(new Problem.ProblemBatchCase(batchNum, caseNum, "", data.toString()));
            }
        }

        return list;
    }

    private ProblemForm problemToProblemForm(Problem p) {
        ProblemForm pf = new ProblemForm();
        pf.setName(p.getName());
        pf.setPrettyName(p.getPrettyName());
        pf.setProblemStatement(p.getProblemStatement());
        pf.setAllowPartial(p.isAllowPartial());
        pf.setTotalScoreWorth(p.getTotalScoreWorth());
        pf.setLimits(new ArrayList<>());
        for (Problem.ProblemLimits l : p.getLimits()) {
            pf.getLimits().add(new ProblemFormLimit(JudgeLanguage.nameToPretty(l.getLang()), l.getTimeLimit(), l.getMemoryLimit()));
        }
        return pf;
    }

    private Problem problemFormToProblem(Problem original, ProblemForm form) {
        Problem problem = original == null ? new Problem() : original;

        problem.setName(form.getName());
        problem.setPrettyName(form.getPrettyName());
        problem.setProblemStatement(form.getProblemStatement());
        problem.setAllowPartial(form.isAllowPartial());
        problem.setTotalScoreWorth(form.getTotalScoreWorth());

        problem.setLimits(new ArrayList<>());
        for (ProblemFormLimit l : form.getLimits()) {
            problem.getLimits().add(new Problem.ProblemLimits(JudgeLanguage.prettyToName(l.getLang()), l.timeLimit, l.memoryLimit));
        }

        problem.setSubmissions(new ArrayList<>());
        return problem;
    }

}
