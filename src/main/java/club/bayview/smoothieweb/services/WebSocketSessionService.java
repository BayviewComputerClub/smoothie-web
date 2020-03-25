package club.bayview.smoothieweb.services;

import club.bayview.smoothieweb.util.NotFoundException;
import club.bayview.smoothieweb.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketSessionService {

    @Autowired
    ReactiveSessionRepository sessionRepository;

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

    /**
     * Given a WebSocketSession, obtain the authentication or empty if null.
     * @param session the websocket session
     * @return the authentication, or empty if it could not be found
     */

    public Mono<Authentication> getAuthentication(WebSocketSession session) {
        return sessionRepository.findById(SessionUtils.getSessionIdFromHeader(session.getHandshakeInfo().getHeaders()))
                .switchIfEmpty(Mono.error(new NotFoundException()))
                .flatMap(s -> {
                    Session sess = (Session) s;
                    // get user session from websocket http headers
                    SecurityContextImpl securityContext = sess.getAttribute("SPRING_SECURITY_CONTEXT");
                    if (securityContext.getAuthentication() == null) {
                        return Mono.empty();
                    } else {
                        return Mono.just(securityContext.getAuthentication());
                    }
                });
    }

    // helper function to take a single message from a websocket session
    public Flux<WebSocketMessage> takeOneMsg(WebSocketSession session) {
        return session.receive().take(1);
    }

    // helper function to set a unicastprocessor for websocket session input
    public Mono<Void> setupInput(WebSocketSession session, UnicastProcessor<WebSocketMessage> inputStream, Flux<?> c) {
        return session.send(inputStream).and(c).then();
    }
}
