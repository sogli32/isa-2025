package com.example.analytics.service;

import com.example.analytics.dto.BenchmarkResult;
import com.example.backend.proto.VideoUploadEventProto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MessageConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumerService.class);

    private final ObjectMapper objectMapper;
    private final List<BenchmarkResult> jsonResults = new ArrayList<>();
    private final List<BenchmarkResult> protobufResults = new ArrayList<>();

    public MessageConsumerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Listener za JSON poruke
     */
    @RabbitListener(queues = "video.upload.json")
    public void receiveJsonMessage(String jsonMessage) {
        long startTime = System.nanoTime();

        try {
            // Deserijalizuj JSON
            Map<String, Object> event = objectMapper.readValue(jsonMessage, Map.class);

            long endTime = System.nanoTime();
            long deserializationTime = endTime - startTime;

            // Izračunaj veličinu poruke
            int messageSize = jsonMessage.getBytes().length;

            BenchmarkResult result = new BenchmarkResult(
                    "JSON",
                    messageSize,
                    deserializationTime,
                    0L // Serijalizacija se desila na publisher strani
            );

            jsonResults.add(result);

            logger.info("📩 JSON received - Video: {}, Size: {} bytes, Deserialization: {} ns",
                    event.get("videoId"), messageSize, deserializationTime);

        } catch (Exception e) {
            logger.error("❌ Failed to process JSON message", e);
        }
    }

    /**
     * Listener za Protobuf poruke
     */
    @RabbitListener(queues = "video.upload.protobuf")
    public void receiveProtobufMessage(byte[] protobufBytes) {
        long startTime = System.nanoTime();

        try {
            // Deserijalizuj Protobuf
            VideoUploadEventProto.UploadEvent event = 
                VideoUploadEventProto.UploadEvent.parseFrom(protobufBytes);

            long endTime = System.nanoTime();
            long deserializationTime = endTime - startTime;

            // Veličina poruke
            int messageSize = protobufBytes.length;

            BenchmarkResult result = new BenchmarkResult(
                    "PROTOBUF",
                    messageSize,
                    deserializationTime,
                    0L // Serijalizacija se desila na publisher strani
            );

            protobufResults.add(result);

            logger.info("📩 Protobuf received - Video: {}, Size: {} bytes, Deserialization: {} ns",
                    event.getVideoId(), messageSize, deserializationTime);

        } catch (Exception e) {
            logger.error("❌ Failed to process Protobuf message", e);
        }
    }

    /**
     * Dobavi JSON rezultate
     */
    public List<BenchmarkResult> getJsonResults() {
        return new ArrayList<>(jsonResults);
    }

    /**
     * Dobavi Protobuf rezultate
     */
    public List<BenchmarkResult> getProtobufResults() {
        return new ArrayList<>(protobufResults);
    }

    /**
     * Resetuj rezultate
     */
    public void clearResults() {
        jsonResults.clear();
        protobufResults.clear();
        logger.info("🔄 Benchmark results cleared");
    }
}
