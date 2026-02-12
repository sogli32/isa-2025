package com.example.backend.controller;

import com.example.backend.dto.CommentResponse;
import com.example.backend.dto.CreateCommentRequest;
import com.example.backend.security.JwtUtil;
import com.example.backend.services.CommentService;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
//@CrossOrigin(origins = "http://localhost:4200")
public class CommentController {

    private final CommentService commentService;
    private final JwtUtil jwtUtil;

    public CommentController(CommentService commentService, JwtUtil jwtUtil) {
        this.commentService = commentService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/{videoId}")
    public ResponseEntity<?> addComment(
            @PathVariable Long videoId,
            @RequestBody CreateCommentRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String userEmail = extractEmailFromToken(authHeader);
            CommentResponse response = commentService.addComment(videoId, userEmail, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add comment: " + e.getMessage());
        }
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long videoId) {
        List<CommentResponse> comments = commentService.getCommentsForVideo(videoId);
        return ResponseEntity.ok(comments);
    }

    private String extractEmailFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Claims claims = jwtUtil.validateToken(token).getBody();
        return claims.getSubject();
    }
}
