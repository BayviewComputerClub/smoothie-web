package club.bayview.models;

import javax.persistence.*;

/**
 * Represents a submission made by a user for a problem.
 */

@Entity
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private JudgeLanguage lang;

    @ManyToOne
    private User user;

    @ManyToOne
    private Problem problem;

    private String code;

    // ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
}
