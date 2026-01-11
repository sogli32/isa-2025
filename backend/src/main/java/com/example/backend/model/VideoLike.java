package com.example.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "video_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"video_id", "user_id"})
})
public class VideoLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public VideoLike() {}

    public VideoLike(Video video, User user) {
        this.video = video;
        this.user = user;
    }

    // Getters i setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
