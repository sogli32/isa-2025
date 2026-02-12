package com.example.backend.services;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.dto.UploadEvent;
import com.example.backend.model.Video;
import com.example.backend.proto.VideoUploadEventProto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoUploadPublisher {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public VideoUploadPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish upload event u JSON formatu
     */
    public void publishJsonEvent(Video video, long fileSize) {
        try {
            UploadEvent event = createUploadEvent(video, fileSize);

            // Serijalizuj u JSON
            String jsonMessage = objectMapper.writeValueAsString(event);

            // Pošalji na RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.VIDEO_UPLOAD_EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_JSON,
                    jsonMessage
            );

            logger.info("✅ Published JSON event for video {}: {} bytes", video.getId(), jsonMessage.length());

        } catch (Exception e) {
            logger.error("❌ Failed to publish JSON event", e);
        }
    }

    /**
     * Publish upload event u Protobuf formatu
     */
    public void publishProtobufEvent(Video video, long fileSize) {
        try {
            UploadEvent event = createUploadEvent(video, fileSize);

            // Konvertuj u Protobuf
            VideoUploadEventProto.UploadEvent protoEvent = VideoUploadEventProto.UploadEvent.newBuilder()
                    .setVideoId(event.getVideoId())
                    .setTitle(event.getTitle())
                    .setUsername(event.getUsername())
                    .setFileSize(event.getFileSize())
                    .setUploadTime(event.getUploadTime())
                    .setLocation(event.getLocation() != null ? event.getLocation() : "")
                    .addAllTags(event.getTags())
                    .build();

            // Serijalizuj u byte array
            byte[] protobufBytes = protoEvent.toByteArray();

            // Pošalji na RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.VIDEO_UPLOAD_EXCHANGE,
                    RabbitMQConfig.ROUTING_KEY_PROTOBUF,
                    protobufBytes
            );

            logger.info("✅ Published Protobuf event for video {}: {} bytes", video.getId(), protobufBytes.length);

        } catch (Exception e) {
            logger.error("❌ Failed to publish Protobuf event", e);
        }
    }

    /**
     * Helper metoda za kreiranje UploadEvent objekta
     */
    private UploadEvent createUploadEvent(Video video, long fileSize) {
        List<String> tags = Arrays.stream(video.getTags().split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        return new UploadEvent(
                video.getId(),
                video.getTitle(),
                video.getUser().getUsername(),
                fileSize,
                LocalDateTime.now().toString(),
                video.getLocation(),
                tags
        );
    }
}
