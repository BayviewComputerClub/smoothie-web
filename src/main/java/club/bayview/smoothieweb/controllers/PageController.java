package club.bayview.smoothieweb.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpMethod;

// Reactive Web Client
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Controller
public class PageController {

    // JSON Objects
    static class PageResponse {
        public boolean status;
        public Page[] pages;
        public String error;
    }

    static class Page {
        public int id;
        public String slug;
        public String date;
        public boolean display_on_nav;
        public int parent;
        public String nav_title;
        public String title;
        public String meta;
        public String content;
    }

    private static String cmsHost = "http://localhost:3000";

    @RequestMapping("/page/{slug}")
    public String requestPage(Model model, @PathVariable("slug") String slug) throws JsonProcessingException {

        WebClient client = WebClient.create(cmsHost);
        WebClient.RequestBodySpec request = client
                .method(HttpMethod.GET)
                .uri("/pages/" + slug);

        String pageJSON = request.exchange().block().bodyToMono(String.class).block();

        ObjectMapper mapper = new ObjectMapper();
        PageResponse pageResponse = mapper.readValue(pageJSON, PageResponse.class);

        if(pageResponse.pages.length == 0) {
            return "404";
        }

        Page page = pageResponse.pages[0];

        model.addAttribute("page", page);
        return "page";
    }
    public static Page[] getNavs(int parent) {
        // HOT PATH (each time the nav bar is rendered) Make sure it's optimized
        WebClient client = WebClient.create(cmsHost);
        WebClient.RequestBodySpec request;

        try {
            request = client
                    .method(HttpMethod.GET)
                    .uri("/navs/" + parent);
        } catch (Exception e) {
            // Return a blank page response if the cms server is offline.
            return new PageResponse().pages;
        }

        return request.exchange().flatMap(res -> res.bodyToMono(String.class))
                .flatMap(pageJson -> {
                    try {
                        return Mono.just(new ObjectMapper().readValue(pageJson, PageResponse.class).pages);
                    } catch (JsonProcessingException e) {
                        return Mono.error(e);
                    }
                })
                .onErrorResume(e -> Mono.just(new Page[0]))
                .block(); // Todo don't block
    }
}