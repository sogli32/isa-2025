package com.example.backend.services;

import com.example.backend.dto.CommentResponse;
import com.example.backend.dto.CreateCommentRequest;
import com.example.backend.model.Comment;
import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.repository.CommentRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VideoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, VideoRepository videoRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    public CommentResponse addComment(Long videoId, String userEmail, CreateCommentRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        Comment comment = new Comment(request.getContent(), user, video);
        commentRepository.save(comment);

        return new CommentResponse(comment.getId(), comment.getContent(), user.getUsername(), comment.getCreatedAt());
    }

    public List<CommentResponse> getCommentsForVideo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));

        return commentRepository.findByVideoOrderByCreatedAtAsc(video)
                .stream()
                .map(c -> new CommentResponse(c.getId(), c.getContent(), c.getUser().getUsername(), c.getCreatedAt()))
                .collect(Collectors.toList());
    }
}
