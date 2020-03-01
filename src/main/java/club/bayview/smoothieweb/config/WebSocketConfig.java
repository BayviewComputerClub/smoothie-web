package club.bayview.smoothieweb.config;

import club.bayview.smoothieweb.controllers.LiveSubmissionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/live-submission", new LiveSubmissionController());

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(map);
        handlerMapping.setOrder(10);
        handlerMapping.setCorsConfigurations(Collections.singletonMap("*", new CorsConfiguration().applyPermitDefaultValues()));
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

}
