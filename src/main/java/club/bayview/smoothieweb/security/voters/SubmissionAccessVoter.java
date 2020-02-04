package club.bayview.smoothieweb.security.voters;

import club.bayview.smoothieweb.models.Role;
import club.bayview.smoothieweb.models.Submission;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;

import java.util.Collection;

/**
 * Controls access to submissions based on user.
 */

public class SubmissionAccessVoter implements AccessDecisionVoter {
    @Override
    public boolean supports(ConfigAttribute attribute) {
        return attribute.getAttribute().equals("SUBMISSION_DEFAULT");
    }

    @Override
    public int vote(Authentication authentication, Object object, Collection collection) {
        Submission s = (Submission) object;

        // admins get first dibs
        if (authentication.getAuthorities().contains(Role.ROLE_ADMIN)) {
            return ACCESS_GRANTED;
        }



        return 0;
    }

    @Override
    public boolean supports(Class clazz) {
        return clazz.isInstance(new Submission());
    }
}
