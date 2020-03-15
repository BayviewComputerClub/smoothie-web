package club.bayview.smoothieweb.api.models;

import club.bayview.smoothieweb.models.Problem;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;

import java.util.List;

@Getter
@Setter
public class APIProblem {
    String name;
    String prettyName;
    List<Problem.ProblemLimits> limits;
    String problemStatement;
    boolean allowPartial;
    int totalPointsWorth;
    int rateOfAC;
    int usersSolved;
    List<String> editorIds;
    long timeCreated;

    public boolean hasPermissionToView(Authentication auth) {
        return true;
    }

    public static APIProblem fromProblem(Problem p) {
        APIProblem np = new APIProblem();
        np.setName(p.getName());
        np.setPrettyName(p.getPrettyName());
        np.setLimits(p.getLimits());
        np.setProblemStatement(p.getProblemStatement());
        //np.setAllowPartial(p.getAllowPartial());
        np.setTotalPointsWorth(p.getTotalPointsWorth());
        np.setRateOfAC(p.getRateOfAC());
        np.setUsersSolved(p.getUsersSolved());
        np.setEditorIds(p.getEditorIds());
        np.setTimeCreated(p.getTimeCreated());
        return np;
    }
}

