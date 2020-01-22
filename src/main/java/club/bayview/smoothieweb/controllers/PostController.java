package club.bayview.smoothieweb.controllers;

import club.bayview.smoothieweb.models.Post;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class PostController {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PostForm {
        private List<String> userCreatorIds;

        private String slug, name, content;

        public PostForm(Post p) {
            userCreatorIds = p.getUserCreatorIds();
            slug = p.getSlug();
            name = p.getName();
            content = p.getContent();
        }

        public Post toPost(Post original) {
            applyToPost(original);
            return original;
        }

        public Post toPost() {
            Post p = new Post();
            applyToPost(p);
            return p;
        }

        private void applyToPost(Post p) {
            p.setUserCreatorIds(userCreatorIds);
            p.setSlug(slug);
            p.setName(name);
            p.setContent(content);
        }
    }


}
