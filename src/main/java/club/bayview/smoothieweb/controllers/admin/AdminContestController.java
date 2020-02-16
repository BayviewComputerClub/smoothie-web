package club.bayview.smoothieweb.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Mono;

@Controller
public class AdminContestController {

    @GetMapping("/contest/{name}/admin")
    public Mono<String> getContestDashboard(@PathVariable String name, Model model) {
        return Mono.just("contest");
    }



}
