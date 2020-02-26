package club.bayview.smoothieweb.controllers.admin;

import club.bayview.smoothieweb.controllers.PageController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Controller
public class AdminPageController {

    public static void updatePage(PageController.PageUpdateRequest pageUpdateRequest) throws Exception {
        WebClient client = WebClient.builder()
                .baseUrl(PageController.cmsHost + "/pages")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();

        String pageJSON = client.put().body(BodyInserters.fromObject(pageUpdateRequest)).exchange().block().bodyToMono(String.class).block();

        ObjectMapper mapper = new ObjectMapper();
        PageController.PageResponse result = mapper.readValue(pageJSON, PageController.PageResponse.class);
    }

    @GetMapping("/admin/pages")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> pagesList(Model model) throws Exception{

        PageController.PageResponse pageResponse;

        try {
            pageResponse = PageController.getPageResponse();
            model.addAttribute("pages", pageResponse.pages);
            return Mono.just("admin/pages");
        } catch (Exception e) {
            e.printStackTrace();
            return Mono.just("error");
        }

    }

    @GetMapping("/admin/pages/{slug}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String pageEdit(Model model, @PathVariable("slug") String slug) throws Exception{

        PageController.PageResponse pageResponse = PageController.getPageResponseBySlugRaw(slug);

        model.addAttribute("page", pageResponse.pages[0]);
        return "admin/page-edit";
    }

    @PostMapping("/admin/pages/{slug}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Mono<String> pageEditPost(Model model, @PathVariable("slug") String slug,
                               @RequestParam("slug") String new_slug,
                               @RequestParam("date") String date,
                               @RequestParam("display_on_nav") boolean display_on_nav,
                               @RequestParam("parent") int parent,
                               @RequestParam("nav_title") String nav_title,
                               @RequestParam("title") String title,
                               @RequestParam("meta") String meta,
                               @RequestParam("content") String content
    ) throws Exception{

        PageController.PageUpdateRequest pageUpdateRequest = new PageController.PageUpdateRequest();
        pageUpdateRequest.slug = slug;
        PageController.Page page = new PageController.Page();
        page.slug = new_slug;
        page.date = date;
        page.display_on_nav = display_on_nav;
        page.parent = parent;
        page.nav_title = nav_title;
        page.title = title;
        page.meta = meta;
        page.content = content;
        pageUpdateRequest.page = page;

        updatePage(pageUpdateRequest);
        return Mono.just("admin/admin");
    }
}
