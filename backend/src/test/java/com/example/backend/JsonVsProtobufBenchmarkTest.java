package com.example.backend;

import com.example.backend.dto.UploadEvent;
import com.example.backend.proto.VideoUploadEventProto;
import com.example.backend.services.VideoUploadPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Benchmark test za poređenje JSON vs Protobuf serijalizacije
 * Zahtev 3.14: Testiranje na minimum 50 poruka
 */
@SpringBootTest
public class JsonVsProtobufBenchmarkTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static final int NUM_ITERATIONS = 50;

    @Test
    public void benchmarkJsonVsProtobuf() throws Exception {
        System.out.println("\n=================================================");
        System.out.println("📊 JSON vs PROTOBUF BENCHMARK TEST");
        System.out.println("=================================================\n");

        List<Long> jsonSerializationTimes = new ArrayList<>();
        List<Long> jsonDeserializationTimes = new ArrayList<>();
        List<Integer> jsonMessageSizes = new ArrayList<>();

        List<Long> protobufSerializationTimes = new ArrayList<>();
        List<Long> protobufDeserializationTimes = new ArrayList<>();
        List<Integer> protobufMessageSizes = new ArrayList<>();

        // Kreiraj test poruke
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            UploadEvent event = createTestEvent(i + 1);

            // ========== JSON TEST ==========
            long jsonSerStart = System.nanoTime();
            String jsonMessage = objectMapper.writeValueAsString(event);
            long jsonSerEnd = System.nanoTime();
            jsonSerializationTimes.add(jsonSerEnd - jsonSerStart);
            jsonMessageSizes.add(jsonMessage.getBytes().length);

            long jsonDeserStart = System.nanoTime();
            UploadEvent deserializedJson = objectMapper.readValue(jsonMessage, UploadEvent.class);
            long jsonDeserEnd = System.nanoTime();
            jsonDeserializationTimes.add(jsonDeserEnd - jsonDeserStart);

            // ========== PROTOBUF TEST ==========
            long protoSerStart = System.nanoTime();
            VideoUploadEventProto.UploadEvent protoEvent = VideoUploadEventProto.UploadEvent.newBuilder()
                    .setVideoId(event.getVideoId())
                    .setTitle(event.getTitle())
                    .setUsername(event.getUsername())
                    .setFileSize(event.getFileSize())
                    .setUploadTime(event.getUploadTime())
                    .setLocation(event.getLocation() != null ? event.getLocation() : "")
                    .addAllTags(event.getTags())
                    .build();
            byte[] protobufBytes = protoEvent.toByteArray();
            long protoSerEnd = System.nanoTime();
            protobufSerializationTimes.add(protoSerEnd - protoSerStart);
            protobufMessageSizes.add(protobufBytes.length);

            long protoDeserStart = System.nanoTime();
            VideoUploadEventProto.UploadEvent deserializedProto = 
                VideoUploadEventProto.UploadEvent.parseFrom(protobufBytes);
            long protoDeserEnd = System.nanoTime();
            protobufDeserializationTimes.add(protoDeserEnd - protoDeserStart);
        }

        // ========== IZRAČUNAJ PROSEKE ==========
        double avgJsonSerTime = jsonSerializationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgJsonDeserTime = jsonDeserializationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgJsonSize = jsonMessageSizes.stream().mapToInt(Integer::intValue).average().orElse(0);

        double avgProtoSerTime = protobufSerializationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgProtoDeserTime = protobufDeserializationTimes.stream().mapToLong(Long::longValue).average().orElse(0);
        double avgProtoSize = protobufMessageSizes.stream().mapToInt(Integer::intValue).average().orElse(0);

        // ========== PRIKAZ REZULTATA ==========
        System.out.println("📝 TEST PARAMETRI:");
        System.out.println("   Broj poruka: " + NUM_ITERATIONS);
        System.out.println();

        System.out.println("📊 JSON REZULTATI:");
        System.out.println("   Prosečna veličina poruke:     " + String.format("%.2f", avgJsonSize) + " bytes");
        System.out.println("   Prosečno vreme serijalizacije:   " + String.format("%.2f", avgJsonSerTime / 1_000) + " μs");
        System.out.println("   Prosečno vreme deserijalizacije: " + String.format("%.2f", avgJsonDeserTime / 1_000) + " μs");
        System.out.println();

        System.out.println("📊 PROTOBUF REZULTATI:");
        System.out.println("   Prosečna veličina poruke:     " + String.format("%.2f", avgProtoSize) + " bytes");
        System.out.println("   Prosečno vreme serijalizacije:   " + String.format("%.2f", avgProtoSerTime / 1_000) + " μs");
        System.out.println("   Prosečno vreme deserijalizacije: " + String.format("%.2f", avgProtoDeserTime / 1_000) + " μs");
        System.out.println();

        System.out.println("📈 POREĐENJE:");
        double sizeReduction = ((avgJsonSize - avgProtoSize) / avgJsonSize) * 100;
        double serSpeedup = (avgJsonSerTime / avgProtoSerTime);
        double deserSpeedup = (avgJsonDeserTime / avgProtoDeserTime);

        System.out.println("   Smanjenje veličine poruke:     " + String.format("%.2f", sizeReduction) + "%");
        System.out.println("   Ubrzanje serijalizacije:       " + String.format("%.2fx", serSpeedup));
        System.out.println("   Ubrzanje deserijalizacije:     " + String.format("%.2fx", deserSpeedup));
        System.out.println();

        System.out.println("✅ ZAKLJUČAK:");
        System.out.println("   Protobuf je " + (sizeReduction > 0 ? "efikasniji" : "manje efikasan") + " od JSON-a");
        System.out.println("   u smislu veličine poruke i brzine serijalizacije/deserijalizacije.");
        System.out.println();

        System.out.println("=================================================\n");
    }

    private UploadEvent createTestEvent(long id) {
        return new UploadEvent(
                id,
                "Test Video " + id,
                "testuser" + id,
                15_000_000L + (id * 100_000), // ~15MB video file
                java.time.LocalDateTime.now().toString(),
                "Belgrade, Serbia",
                Arrays.asList("tag1", "tag2", "tag3", "tutorial", "coding")
        );
    }
}
