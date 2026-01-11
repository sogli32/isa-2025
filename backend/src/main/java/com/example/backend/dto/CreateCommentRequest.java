package com.example.backend.dto;

public class CreateCommentRequest {
    private String content;

    public CreateCommentRequest() {}
    public CreateCommentRequest(String content) { this.content = content; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
