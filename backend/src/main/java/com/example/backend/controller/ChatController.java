package com.example.backend.controller;

import com.example.backend.model.ChatMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    /**
     * Endpoint za slanje poruka u čet za određeni video
     * Klijent šalje poruku na: /app/chat/{videoId}/sendMessage
     * Server broadcasta na: /topic/chat/{videoId}
     */
    @MessageMapping("/chat/{videoId}/sendMessage")
    @SendTo("/topic/chat/{videoId}")
    public ChatMessage sendMessage(
            @DestinationVariable Long videoId,
            @Payload ChatMessage chatMessage) {
        
        // Postavi timestamp na serveru
        chatMessage.setTimestamp(java.time.LocalDateTime.now().toString());
        chatMessage.setVideoId(videoId);
        
        return chatMessage;
    }

    /**
     * Endpoint za dodavanje korisnika u čet
     * Klijent šalje poruku na: /app/chat/{videoId}/addUser
     * Server broadcasta na: /topic/chat/{videoId}
     */
    @MessageMapping("/chat/{videoId}/addUser")
    @SendTo("/topic/chat/{videoId}")
    public ChatMessage addUser(
            @DestinationVariable Long videoId,
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor) {
        
        // Add username in web socket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("videoId", videoId);
        
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        chatMessage.setTimestamp(java.time.LocalDateTime.now().toString());
        chatMessage.setVideoId(videoId);
        
        return chatMessage;
    }
}
