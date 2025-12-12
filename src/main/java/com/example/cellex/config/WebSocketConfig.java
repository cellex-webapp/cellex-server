package com.example.cellex.config;

import com.example.cellex.services.auth.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket với STOMP protocol cho chức năng chat realtime
 * 
 * Endpoints:
 * - /ws: WebSocket endpoint chính để client kết nối
 * 
 * Message Broker:
 * - /topic: Dùng cho broadcast messages (nhiều người nhận)
 * - /queue: Dùng cho point-to-point messages (một người nhận)
 * - /app: Prefix cho các message gửi đến server
 * 
 * @author Cellex Team
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker cho các destinations bắt đầu bằng /topic và /queue
        // /topic dùng cho pub/sub (1-to-many)
        // /queue dùng cho point-to-point (1-to-1)
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Prefix cho các message từ client gửi đến @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // Prefix cho user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký WebSocket endpoint
        // Client sẽ kết nối tới ws://localhost:8080/ws
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Cho phép tất cả origins (production nên restrict)
                .withSockJS();  // Enable SockJS fallback cho browsers không hỗ trợ WebSocket
        
        // Endpoint không có SockJS (cho native WebSocket clients)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Xử lý authentication khi client CONNECT
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            // Giải mã JWT token để lấy user information
                            String userEmail = jwtService.extractUsername(token);
                            
                            if (userEmail != null) {
                                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                                
                                if (userDetails != null && jwtService.isTokenValid(token, userDetails)) {
                                    UsernamePasswordAuthenticationToken authentication = 
                                            new UsernamePasswordAuthenticationToken(
                                                    userDetails, 
                                                    null, 
                                                    userDetails.getAuthorities()
                                            );
                                    
                                    SecurityContextHolder.getContext().setAuthentication(authentication);
                                    accessor.setUser(authentication);
                                    
                                    log.info("WebSocket connection authenticated for user: {}", userEmail);
                                }
                            }
                        } catch (Exception e) {
                            log.error("WebSocket authentication failed: {}", e.getMessage());
                        }
                    } else {
                        log.warn("WebSocket connection without Authorization header");
                    }
                }
                
                return message;
            }
        });
    }
}
