package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.TestData;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.util.NotFoundException;
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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
public class AdminProblemTestDataController {

    @Autowired
    SmoothieProblemService problemService;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestDataForm {
        private MultipartFile testData;
    }

    @GetMapping("/problem/{name}/edit/testdata")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getEditTestDataRoute(@PathVariable String name, Model model) {

        return problemService.findProblemByName(name).switchIfEmpty(Mono.error(new NotFoundException())).flatMap(p -> {

            model.addAttribute("problem", p);
            model.addAttribute("testDataForm", new TestDataForm());

            return Mono.just("admin/edit-problem-testdata");
        }).onErrorResume(e -> Mono.just("404"));
    }

    @PostMapping("/problem/{name}/edit/testdata")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postEditTestDataRoute(@Valid TestDataForm form, BindingResult result, @PathVariable String name, Model model) {

        return problemService.findProblemByName(name).switchIfEmpty(Mono.error(new NotFoundException())).flatMap(p -> {

            if (result.hasErrors()) {
                model.addAttribute("problem", p);
                model.addAttribute("testDataForm", new TestDataForm());
                return Mono.just("admin/edit-problem-testdata");
            }

            try {
                return problemService.saveTestDataForProblem(getTestCasesFromZip(form.getTestData()), p)
                        .then(Mono.just("redirect:/problem/" + name + "/manage"));
            } catch (Exception e) {
                return Mono.error(e);
            }

        }).onErrorResume(e -> e instanceof NotFoundException ? Mono.just("404") : Mono.just("500"));
    }

    private TestData getTestCasesFromZip(MultipartFile file) throws IOException {
        if (file == null) return new TestData(new ArrayList<>());

        List<List<Problem.ProblemBatchCase>> list = new ArrayList<>();
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(file.getInputStream()));
        ZipEntry entry;

        // TODO make this better with error checking and instruction file for points
        // format: 0-0.in, 0-0.out, 0-1.in, 0-1.out, etc...
        while ((entry = zis.getNextEntry()) != null) {

            // get vals
            String[] spl = entry.getName().split("-");
            int batchNum = Integer.parseInt(spl[0]), caseNum = Integer.parseInt(spl[1].split("\\.")[0]);
            boolean isInput = entry.getName().split("\\.")[1].equals("in");

            // get data
            byte[] buffer = new byte[1024];
            StringBuilder data = new StringBuilder();
            while (zis.read(buffer, 0, 1024) >= 0) {
                data.append(new String(buffer, 0, 1024).replace("\u0000", "").replace("\\u0000", ""));
            }
            String dataString = data.toString();

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
                    if (isInput) c.setInput(dataString);
                    else c.setExpectedOutput(dataString);
                }
            }

            if (!found) {
                if (isInput) list.get(batchNum).add(new Problem.ProblemBatchCase(batchNum, caseNum, dataString, ""));
                else list.get(batchNum).add(new Problem.ProblemBatchCase(batchNum, caseNum, "", dataString));
            }
        }

        // sort cases into order
        for (var cases : list) {
            Collections.sort(cases);
        }

        return new TestData(list);
    }

}
