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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Controller
public class AdminProblemTestDataController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothieProblemService problemService;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestDataFileForm {
        private Mono<MultipartFile> testData;
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

    @GetMapping(path = "/problem/{name}/edit/testdata/download", produces = "application/octet-stream")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<ResponseEntity<byte[]>> getDownloadTestDataRoute(@PathVariable String name) {

        var re = ResponseEntity
                .ok().cacheControl(CacheControl.noCache())
                .header("Content-Type", "application/octet-stream")
                .header("Content-Disposition", "attachment; filename=" + name + ".zip");

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    try {
                        return problemService.findProblemTestData(p.getTestDataId());
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                })
                .flatMap(testData -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ZipOutputStream zos = new ZipOutputStream(baos);

                    try {
                        for (var batch : testData.getBatchList()) {
                            for (var batchCase : batch.getCaseList()) {
                                ZipEntry zei = new ZipEntry(batch.getBatchNum() + "-" + batchCase.getCaseNum() + ".in"),
                                        zeo = new ZipEntry(batch.getBatchNum() + "-" + batchCase.getCaseNum() + ".out");
                                zos.putNextEntry(zei);
                                zos.write(batchCase.getInput().getBytes());
                                zos.closeEntry();
                                zos.putNextEntry(zeo);
                                zos.write(batchCase.getExpectedOutput().getBytes());
                                zos.closeEntry();
                            }
                        }
                    } catch (Exception e) {
                        return Mono.error(e);
                    } finally {
                        try {
                            zos.close();
                        } catch (IOException e) {
                            return Mono.error(e);
                        }
                    }

                    return Mono.just(re.body(baos.toByteArray()));
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(re.body(new byte[0]));
                });
    }

    @PostMapping(path = "/problem/{name}/edit/testdata/file")
    @PreAuthorize("hasRole('ROLE_EDITOR')")
    public Mono<String> postEditTestDataFileRoute(@RequestPart("testData") Mono<FilePart> file, @PathVariable String name, Model model) {

        InputStream is = new SequenceInputStream(Collections.enumeration(file.flatMapMany(Part::content).map(DataBuffer::asInputStream).toStream().collect(Collectors.toList())));

        return problemService.findProblemByName(name)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(p -> {
                    try {

                        StoredTestData.TestData testData = getTestCasesFromZip(is);
                        Problem.TestDataWrapper w = new Problem.TestDataWrapper(new ArrayList<>(), testData);

                        for (var b : testData.getBatchList()) {
                            w.getBatches().add(new Problem.ProblemBatch((int)b.getBatchNum(), b.getCaseCount(), b.getCaseCount())); // TODO partial
                        }

                        return problemService.saveTestDataForProblem(w, p)
                                .then(Mono.just("redirect:/problem/" + name + "/manage"));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }

                }).onErrorResume(e -> ErrorCommon.handle404(e, logger, "POST /problem/{name}/edit/testdata/file route exception: "));
    }

    private StoredTestData.TestData getTestCasesFromZip(InputStream file) throws IOException {
        var testData = StoredTestData.TestData.newBuilder();
        if (file == null) return testData.build();

        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(file));
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
            byte[] buffer = new byte[(int)entry.getSize() >= 0 ? (int) entry.getSize() : (int) 10e6]; // 10mb
            long size = entry.getSize(), read = 0;
            while (read < size) {
                read += zis.read(buffer, (int)read, (int)(size - read));
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

        // sort cases into order and build test data for storing in db
        for (int i = 0; i < builders.size(); i++) {
            var cases = builders.get(i);
            cases.sort((a, b) -> a.getCaseNum() > b.getCaseNum() ? 1 : -1);
            var batch = StoredTestData.TestDataBatch.newBuilder();
            for (var tdCase : cases) {
                batch.addCase(tdCase.build());
            }
            batch.setBatchNum(i);
            batch.setPointsWorth(cases.size());
            testData.addBatch(batch.build());
        }

        return testData.build();
    }

}
