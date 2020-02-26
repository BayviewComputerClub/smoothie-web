package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.controllers.PostController;
import club.bayview.smoothieweb.models.Post;
import club.bayview.smoothieweb.services.SmoothiePostService;
import club.bayview.smoothieweb.util.ErrorCommon;
import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.SmUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

public class AdminPostController {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    SmoothiePostService postService;

    @GetMapping("/admin/new-post")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getNewAdminGlobalPost(Model model) {
        model.addAttribute("form", new PostController.PostForm());
        return Mono.just("admin/new-post");
    }

    @GetMapping("/admin/post/{slug}/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> getEditAdminGlobalPost(@PathVariable String id, Model model) {
        return postService.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(post -> {
                    model.addAttribute("form", new PostController.PostForm(post));
                    return Mono.just("admin/new-post");
                })
                .onErrorResume(e -> ErrorCommon.handle404(e, logger, "GET /admin/{post}/{slug}/{id} route exception: "));
    }

    @PostMapping("/admin/new-post")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> postNewAdminGlobalPost(PostController.PostForm form, BindingResult res, Model model) {
        if (res.hasErrors()) {
            // TODO
            model.addAttribute("form", form);
            return Mono.just("admin/new-post");
        }

        Post p = form.toPost();
        p.setGlobalScope(true);
        p.setCreated(SmUtil.getCurrentUnix());
        p.setLastEdited(SmUtil.getCurrentUnix());

        return postService.savePost(p).flatMap(post -> Mono.just("redirect:/post/global/" + post.getSlug() + "/" + post.getId()));
    }

    @PostMapping("/admin/post/{slug}/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> postEditAdminGlobalPost(@PathVariable String id, PostController.PostForm form, BindingResult res, Model model) {
        if (res.hasErrors()) {
            model.addAttribute("form");
            return Mono.just("admin/new-post");
        }

        return postService.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(post -> {
                    form.toPost(post);
                    post.setLastEdited(SmUtil.getCurrentUnix());
                    return postService.savePost(post);
                })
                .flatMap(post -> Mono.just("redirect:/post/global/" + post.getSlug() + "/" + post.getId()))
                .onErrorResume(e -> ErrorCommon.handle404(e, logger, "POST /admin/post/{slug}/{id} route exception: "));
    }


}
