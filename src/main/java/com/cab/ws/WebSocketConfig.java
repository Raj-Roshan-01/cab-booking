package com.cab.ws;

import com.cab.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.*;
import java.util.List;

@Configuration @EnableWebSocketMessageBroker @RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtService jwt;

    @Override public void registerStompEndpoints(StompEndpointRegistry r) { r.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS(); }

    @Override public void configureMessageBroker(MessageBrokerRegistry r) { r.enableSimpleBroker("/topic"); r.setApplicationDestinationPrefixes("/app"); }

    /** JWT is required on STOMP CONNECT; /topic/sos is admin-only. */
    @Override public void configureClientInboundChannel(ChannelRegistration reg) {
        reg.interceptors(new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> m, MessageChannel ch) {
                StompHeaderAccessor a = MessageHeaderAccessor.getAccessor(m, StompHeaderAccessor.class);
                if (a == null) return m;
                if (StompCommand.CONNECT.equals(a.getCommand())) {
                    String h = a.getFirstNativeHeader("Authorization");
                    if (h == null || !h.startsWith("Bearer ")) throw new MessagingException("Unauthorized");
                    try {
                        Claims c = jwt.parse(h.substring(7));
                        a.setUser(new UsernamePasswordAuthenticationToken(c.getSubject(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + c.get("role", String.class)))));
                    } catch (JwtException e) { throw new MessagingException("Invalid token"); }
                } else if (StompCommand.SUBSCRIBE.equals(a.getCommand()) && "/topic/sos".equals(a.getDestination())) {
                    Authentication au = (Authentication) a.getUser();
                    if (au == null || au.getAuthorities().stream().noneMatch(x -> x.getAuthority().equals("ROLE_ADMIN")))
                        throw new MessagingException("Admins only");
                }
                return m;
            }
        });
    }
}
