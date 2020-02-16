package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.testdata.StoredTestData;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NotFoundException;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Controller
public class AdminProblemTestDataController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieProblemService problemService;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestDataFileForm {
        private MultipartFile testData;
    }

    @GetMapping("/problem/{name}/edit/testdata")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getEditTestDataRoute(@PathVariable String name, Model model) {
        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    return Mono.just("admin/edit-problem-testdata");
                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /problem/{name}/edit/testdata route exception: "));
    }

    @GetMapping("/problem/{name}/edit/testdata/table")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getEditTestDataTableRoute(@PathVariable String name, Model model) {

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    model.addAttribute("testDataForm", new TestDataFileForm());

                    return Mono.just("admin/edit-problem-testdata-table");
                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /problem/{name}/edit/testdata/table route exception: "));
    }

    @GetMapping("/problem/{name}/edit/testdata/file")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> getEditTestDataFileRoute(@PathVariable String name, Model model) {

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    model.addAttribute("problem", p);
                    model.addAttribute("testDataForm", new TestDataFileForm());

                    return Mono.just("admin/edit-problem-testdata-file");
                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /problem/{name}/edit/testdata/file route exception: "));
    }

    @PostMapping("/problem/{name}/edit/testdata/file")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postEditTestDataFileRoute(@Valid AdminProblemTestDataController.TestDataFileForm form, BindingResult result, @PathVariable String name, Model model) {

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    if (result.hasErrors()) {
                        model.addAttribute("problem", p);
                        model.addAttribute("testDataForm", new TestDataFileForm());
                        return Mono.just("admin/edit-problem-testdata-file");
                    }

                    try {
                        StoredTestData.TestData testData = getTestCasesFromZip(form.getTestData());
                        Problem.TestDataWrapper w = new Problem.TestDataWrapper(new ArrayList<>(), testData);

                        for (var b : testData.getBatchList()) {
                            w.getBatches().add(new Problem.ProblemBatch(b.getBatchNum(), 0)); // TODO partial
                        }

                        return problemService.saveTestDataForProblem(w, p)
                                .then(Mono.just("redirect:/problem/" + name + "/manage"));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }

                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "POST /problem/{name}/edit/testdata/file route exception: "));
    }

    private StoredTestData.TestData getTestCasesFromZip(MultipartFile file) throws IOException {
        var testData = StoredTestData.TestData.newBuilder();
        if (file == null) return testData.build();

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(file.getInputStream()));
        ZipEntry entry;

        List<List<StoredTestData.TestDataBatchCase.Builder>> builders = new ArrayList<>();

        // TODO make this better with error checking and instruction file for points
        // format: 0-0.in, 0-0.out, 0-1.in, 0-1.out, etc...
        while ((entry = zis.getNextEntry()) != null) {

            // get vals
            String[] spl = entry.getName().split("-");
            int batchNum = Integer.parseInt(spl[0]), caseNum = Integer.parseInt(spl[1].split("\\.")[0]);
            boolean isInput = entry.getName().split("\\.")[1].equals("in");

            // get data (may have to check if this can actually support gb of data)
            byte[] buffer = new byte[(int) entry.getSize()];
            long size = entry.getSize(), read = 0;
            while (read < size) {
                read += zis.read(buffer, (int) read, (int) (size - read));
            }

            String dataString = new String(buffer);

            // add to list
            boolean found = false;

            if (batchNum >= builders.size()) {
                for (int i = builders.size(); i <= batchNum; i++) {
                    builders.add(new ArrayList<>());
                }
            }

            for (var c : builders.get(batchNum)) {
                if (c.getCaseNum() == caseNum) {
                    found = true;
                    if (isInput) c.setInput(dataString);
                    else c.setExpectedOutput(dataString);
                }
            }

            if (!found) {
                if (isInput) {
                    builders.get(batchNum).add(StoredTestData.TestDataBatchCase.newBuilder()
                            .setBatchNum(batchNum)
                            .setCaseNum(caseNum)
                            .setInput(dataString));
                } else {
                    builders.get(batchNum).add(StoredTestData.TestDataBatchCase.newBuilder()
                            .setBatchNum(batchNum)
                            .setCaseNum(caseNum)
                            .setExpectedOutput(dataString));
                }
            }
        }

        // sort cases into order
        for (var cases : builders) {
            cases.sort((a, b) -> a.getCaseNum() > b.getCaseNum() ? 1 : -1);
            var batch = StoredTestData.TestDataBatch.newBuilder();
            for (var tdCase : cases) {
                batch.addCase(tdCase.build());
            }
            testData.addBatch(batch.build());
        }

        return testData.build();
    }

}
