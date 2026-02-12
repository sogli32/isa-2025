package com.example.backend.config;

import com.example.backend.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    /**
     * Kada se korisnik diskonektuje, pošalji LEAVE poruku svim ostalim korisnicima
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        Long videoId = (Long) headerAccessor.getSessionAttributes().get("videoId");

        if (username != null && videoId != null) {
            logger.info("User disconnected from chat: {} in video: {}", username, videoId);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(ChatMessage.MessageType.LEAVE);
            chatMessage.setSender(username);
            chatMessage.setVideoId(videoId);
            chatMessage.setTimestamp(java.time.LocalDateTime.now().toString());

            // Broadcasta LEAVE poruku svim korisnicima tog videa
            messagingTemplate.convertAndSend("/topic/chat/" + videoId, chatMessage);
        }
    }
}
