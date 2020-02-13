package club.bayview.smoothieweb.security.voters;

import club.bayview.smoothieweb.models.Problem;
import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.Submission;
import club.bayview.smoothieweb.models.User;
import club.bayview.smoothieweb.services.SmoothieProblemService;
import club.bayview.smoothieweb.services.SmoothieSubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

import java.util.Collection;

/**
 * Controls access to submissions based on user.
 * TODO does not initialize
 */

public class SubmissionAccessVoter implements AccessDecisionVoter<String> {

    @Autowired
    SmoothieProblemService problemService;

    @Autowired
    SmoothieSubmissionService submissionService;

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return attribute.getAttribute().equals("SUBMISSION_DEFAULT");
    }

    @Override
    public int vote(Authentication authentication, String submissionId, Collection collection) {
        Submission s = submissionService.findSubmissionById(submissionId).block();

        // admins are automatically allowed to see
        if (authentication.getAuthorities().contains(Role.ROLE_ADMIN)) {
            return ACCESS_GRANTED;
        }

        User user = (User) authentication.getPrincipal();
        // if it is the user that submitted
        if (s.getUserId().equals(user.getId())) {
            return ACCESS_GRANTED;
        }
        // if the user has solved the problem
        if (user.getSolved().contains(s.getProblemId())) {
            return ACCESS_GRANTED;
        }

        Problem p = problemService.findProblemById(s.getProblemId()).block();
        // if the problem does not exist
        if (p == null) {
            return ACCESS_GRANTED;
        }
        // if the user is an editor
        if (p.getEditorIds().contains(user.getId())) {
            return ACCESS_GRANTED;
        }
        // deny otherwise
        return ACCESS_DENIED;
    }

    @Override
    public boolean supports(Class clazz) {
        return clazz.isInstance(new Submission());
    }
}
