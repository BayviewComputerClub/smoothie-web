package club.bayview.smoothieweb.services;

import org.springframework.core.io.buffer.*;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.UnicastProcessor;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketSessionService {

    ConcurrentHashMap<String, ConcurrentHashMap<String, UnicastProcessor<WebSocketMessage>>> sessions = new ConcurrentHashMap<>();

    public void sendToClients(String route, String jsonData) {
        if (sessions.containsKey(route)) {

            DefaultDataBufferFactory f  = new DefaultDataBufferFactory();
            sessions.get(route).forEach((id, session) -> session.onNext(new WebSocketMessage(WebSocketMessage.Type.TEXT, f.wrap(jsonData.getBytes()))));
        }
    }

    public void removeSession(String route, String sessionId) {
        if (sessions.containsKey(route)) {
            sessions.get(route).remove(sessionId);
        }
    }

    public void addSession(String route, WebSocketSession session, UnicastProcessor<WebSocketMessage> messagePublisher) {
        if (!sessions.containsKey(route))
            sessions.put(route, new ConcurrentHashMap<>());

        sessions.get(route).put(session.getId(), messagePublisher);
    }

}
