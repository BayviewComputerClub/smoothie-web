package club.bayview.smoothieweb.api.models;

import club.bayview.smoothieweb.models.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIUser {
    String handle;
    String username;
    String email;
    String description;
    double points;

    public static APIUser fromUser(User u) {
        APIUser nu = new APIUser();
        nu.setHandle(u.getHandle());
        nu.setUsername(u.getUsername());
        nu.setEmail(u.getEmail());
        nu.setDescription(u.getDescription());
        nu.setPoints(u.getPoints());
        return nu;
    }
}
