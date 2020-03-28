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

    // <session id, authentication>
    ConcurrentHashMap<String, Authentication> sessionAuth = new ConcurrentHashMap<>();

    // <route, <session id, input stream>>
    ConcurrentHashMap<String, ConcurrentHashMap<String, UnicastProcessor<WebSocketMessage>>> sessions = new ConcurrentHashMap<>();

    public interface AuthenticationVerifier {
        boolean hasPermission(Authentication auth);
    }

    /**
     * Send data to all websocket sessions under a route.
     * @param route the route to send to
     * @param jsonData the data to send
     */

    public void sendToClients(String route, String jsonData) {
        sendToClients(route, jsonData, auth -> true);
    }

    /**
     * Send data to all websocket sessions under a route, with verification of authentication.
     * @param route the route to send to
     * @param jsonData the data to send
     * @param authenticationVerifier lambda that checks permission
     */

    public void sendToClients(String route, String jsonData, AuthenticationVerifier authenticationVerifier) {
        if (sessions.containsKey(route)) {
            DefaultDataBufferFactory f  = new DefaultDataBufferFactory();
            sessions.get(route).forEach((id, session) -> {
                // check if it has permission
                if (sessionAuth.get(id) != null || authenticationVerifier.hasPermission(sessionAuth.get(id))) {
                    session.onNext(new WebSocketMessage(WebSocketMessage.Type.TEXT, f.wrap(jsonData.getBytes())));
                }
            });
        }
    }

    /**
     * Removes a session and corresponding authentication from map.
     * @param route the route that the session is on
     * @param sessionId the websocket session id
     */

    public void removeSession(String route, String sessionId) {
        if (sessions.containsKey(route)) {
            sessions.get(route).remove(sessionId);
        }
        sessionAuth.remove(sessionId);
    }

    /**
     * Add a websocket session with its message publisher to allow for other processes to stream input.
     * @param route the route that senders have to give to send to this websocket session
     * @param session the websocket session
     * @param messagePublisher the input stream
     */

    public void addSession(String route, WebSocketSession session, UnicastProcessor<WebSocketMessage> messagePublisher) {
        if (!sessions.containsKey(route))
            sessions.put(route, new ConcurrentHashMap<>());

        sessions.get(route).put(session.getId(), messagePublisher);
    }

    /**
     * Add a websocket session and corresponding {@link Authentication} with its message publisher to allow for other processes to stream input.
     * @param route the route that senders have to give to send to this websocket session
     * @param session the websocket session
     * @param messagePublisher the input stream
     * @param auth the corresponding authentication for this session
     */

    public void addSession(String route, WebSocketSession session, UnicastProcessor<WebSocketMessage> messagePublisher, Authentication auth) {
        if (auth != null) sessionAuth.put(session.getId(), auth);
        addSession(route, session, messagePublisher);
    }

    /**
     * @return websocket session map
     */

    public ConcurrentHashMap<String, ConcurrentHashMap<String, UnicastProcessor<WebSocketMessage>>> getSessions() {
        return sessions;
    }

    /**
     * @return websocket session authentication map
     */

    public ConcurrentHashMap<String, Authentication> getSessionAuths() {
        return sessionAuth;
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
                    if (securityContext == null || securityContext.getAuthentication() == null) {
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
    // helper function to set a unicastprocessor for websocket session input
    public Mono<Void> setupInput(WebSocketSession session, UnicastProcessor<WebSocketMessage> inputStream, Mono<?> c) {
        return session.send(inputStream).and(c).then();
    }
}
