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

@Controller
public class PageController {
    @RequestMapping("/page/{slug}")
    public String requestPage(Model model, @PathVariable("slug") String slug) throws JsonProcessingException {

        WebClient client = WebClient.create("http://localhost:3000");

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

}

// JSON Objects
class PageResponse {
    public boolean status;
    public Page[] pages;
}

class Page {
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