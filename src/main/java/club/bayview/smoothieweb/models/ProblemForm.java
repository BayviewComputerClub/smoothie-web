package club.bayview.smoothieweb.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class ProblemForm {
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemFormLimit {
        private String lang;
        private double timeLimit, memoryLimit; // time limit in seconds, memory limit in mb
    }

    @NotBlank
    @Size(min = 2)
    private String name,
            prettyName;

    @NotEmpty
    private String problemStatement, renderedProblemStatement;

    @NotNull
    private boolean visibleToPublic;

    @NotNull
    private boolean allowPartial;
    
    @NotNull 
    private String graderType;

    @NotNull
    @Min(0)
    private long totalScoreWorth;

    @NotNull
    private List<ProblemFormLimit> limits;

    public static ProblemForm fromProblem(Problem p) {
        ProblemForm pf = new ProblemForm();
        pf.setName(p.getName());
        pf.setGraderType(p.getGraderType());
        pf.setPrettyName(p.getPrettyName());
        pf.setProblemStatement(p.getProblemStatement());
        pf.setRenderedProblemStatement(p.getRenderedProblemStatement());
        pf.setVisibleToPublic(p.isVisibleToPublic());
        pf.setAllowPartial(p.isAllowPartial());
        pf.setTotalScoreWorth(p.getScoreMultiplier());
        pf.setLimits(new ArrayList<>());
        for (Problem.ProblemLimits l : p.getLimits()) {
            pf.getLimits().add(new ProblemFormLimit(JudgeLanguage.nameToPretty(l.getLang()), l.getTimeLimit(), l.getMemoryLimit()));
        }
        return pf;
    }

    public Problem toProblem(Problem original) {
        Problem problem = original == null ? new Problem() : original;

        problem.setName(this.getName());
        problem.setPrettyName(this.getPrettyName());
        problem.setGraderType(this.getGraderType());
        problem.setProblemStatement(this.getProblemStatement());
        problem.setRenderedProblemStatement(this.getRenderedProblemStatement());
        problem.setVisibleToPublic(this.isVisibleToPublic());
        problem.setAllowPartial(this.isAllowPartial());
        problem.setScoreMultiplier(this.getTotalScoreWorth());

        problem.setLimits(new ArrayList<>());
        for (ProblemFormLimit l : this.getLimits()) {
            problem.getLimits().add(new Problem.ProblemLimits(JudgeLanguage.prettyToName(l.getLang()), l.timeLimit, l.memoryLimit));
        }

        return problem;
    }

    public static ProblemForm defaultProblem;

    static {
        defaultProblem = new ProblemForm();
        defaultProblem.name = "";
        defaultProblem.prettyName = "";
        defaultProblem.graderType = "endtrim";
        defaultProblem.visibleToPublic = true;
        defaultProblem.allowPartial = false;
        defaultProblem.totalScoreWorth = 1;
        defaultProblem.limits = Arrays.asList(new ProblemFormLimit(JudgeLanguage.ALL.getPrettyName(), 1.0, 256));
        defaultProblem.problemStatement = "This is the problem statement.\n" +
                "\n" +
                "Please solve $$a + b = c$$.\n" +
                "\n" +
                "## Input Specification\n" +
                "The first line of input contains the integers $$a$$ and $$b$$ which are the integers to add.\n" +
                "\n" +
                "\n" +
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
}
