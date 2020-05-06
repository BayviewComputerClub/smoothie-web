package club.bayview.smoothieweb.api.models;

import club.bayview.smoothieweb.models.Contest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIContest {
    String id;
    String name;

    String prettyName;
    String description, renderedDescription;

    private long timeStart, // when the contest allows people to join and submit (millisecond epoch)
            timeEnd, // when the contest closes submissions (millisecond epoch)
            submissionPeriod, // contest length (minutes) for when people can submit from when they first "join" the contest; set to 0 to allow submissions throughout the contest length
            timeCreated; // when the contest was created

    private boolean enabled,
            visibleToPublic,
            timeMatters, // time adds points (breaks ties)
            hiddenLeaderBoard; // TODO


    public static APIContest fromContest(Contest c) {
        APIContest nc = new APIContest();

        nc.setId(c.getId());
        nc.setName(c.getName());
        nc.setPrettyName(c.getPrettyName());
        nc.setDescription(c.getDescription());
        nc.setRenderedDescription(c.getRenderedDescription());
        nc.setTimeStart(c.getTimeStart());
        nc.setTimeEnd(c.getTimeEnd());
        nc.setSubmissionPeriod(c.getSubmissionPeriod());
        nc.setTimeCreated(c.getTimeCreated());

        nc.setEnabled(c.isEnabled());
        nc.setVisibleToPublic(c.isVisibleToPublic());
        nc.setTimeMatters(c.isTimeMatters());
        nc.setHiddenLeaderBoard(c.isHiddenLeaderBoard());

        return nc;
    }
}
