package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.models.Post;
import club.bayview.smoothieweb.models.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class SmoothiePostService {

    @Autowired
    PostRepository postRepository;

    public Flux<Post> findGlobalPosts() {
        return postRepository.findByGlobalScopeOrderByCreatedDesc(true);
    }

    public Flux<Post> findByUserGroupId(String userGroupId) {
        return postRepository.findByUserGroupIdOrderByCreatedDesc(userGroupId);
    }

}
