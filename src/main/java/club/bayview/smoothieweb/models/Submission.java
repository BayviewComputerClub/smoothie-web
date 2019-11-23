package club.bayview.smoothieweb.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a submission made by a user for a problem.
 */

@Document
public class Submission {

    @Id
    private String id;

    private JudgeLanguage lang;

    @DBRef
    private User user;

    @DBRef
    private Problem problem;

    private String code;
    private Long timeSubmitted;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    public String getId() {
        return id;
    }

    public JudgeLanguage getLang() {
        return lang;
    }

    public void setLang(JudgeLanguage lang) {
        this.lang = lang;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getTimeSubmitted() {
        return timeSubmitted;
    }

    public void setTimeSubmitted(Long timeSubmitted) {
        this.timeSubmitted = timeSubmitted;
    }


}
