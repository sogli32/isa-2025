package com.example.backend;

import com.example.backend.controller.VideoController;
import com.example.backend.security.JwtUtil;
import com.example.backend.services.GeolocationService;
import com.example.backend.services.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


class VideoStreamingControllerTest {

    private VideoController videoController;
    private StubVideoService stubVideoService;

    @BeforeEach
    void setUp() {
        stubVideoService = new StubVideoService();


        JwtUtil jwtUtil = new JwtUtil();
        GeolocationService geoService = new GeolocationService();

        videoController = new VideoController(stubVideoService, jwtUtil, geoService);
    }

    // ===== streamVideo() testovi =====

    @Test
    void testStreamVideo_Available_Returns200WithVideoData() {
        // Given - video je dostupan i fajl postoji
        stubVideoService.setVideoAvailable(true);
        stubVideoService.setVideoBytes(new byte[]{10, 20, 30, 40, 50});

        // When
        ResponseEntity<?> response = videoController.streamVideo(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        byte[] body = (byte[]) response.getBody();
        assertNotNull(body);
        assertEquals(5, body.length);

        // Proveri headere
        HttpHeaders headers = response.getHeaders();
        assertEquals("video/mp4", headers.getContentType().toString());
        assertEquals(5, headers.getContentLength());
        assertEquals("bytes", headers.getFirst("Accept-Ranges"));
    }

    @Test
    void testStreamVideo_NotAvailable_Returns403() {
        // Given - video nije dostupan (zakazan za buducnost)
        stubVideoService.setVideoAvailable(false);

        // When
        ResponseEntity<?> response = videoController.streamVideo(1L);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("nije dostupan"));
    }

    @Test
    void testStreamVideo_VideoNotFound_Returns404() {
        // Given - video ne postoji
        stubVideoService.setThrowNotFound(true);

        // When
        ResponseEntity<?> response = videoController.streamVideo(999L);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testStreamVideo_FileIOError_Returns500() {
        // Given - video postoji ali fajl ne moze da se ucita
        stubVideoService.setVideoAvailable(true);
        stubVideoService.setThrowIOException(true);

        // When
        ResponseEntity<?> response = videoController.streamVideo(1L);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Failed to load video"));
    }

    @Test
    void testStreamVideo_LargeFile_CorrectContentLength() {
        // Given - veliki video fajl
        byte[] largeData = new byte[10 * 1024 * 1024]; // 10 MB
        stubVideoService.setVideoAvailable(true);
        stubVideoService.setVideoBytes(largeData);

        // When
        ResponseEntity<?> response = videoController.streamVideo(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(10 * 1024 * 1024, response.getHeaders().getContentLength());
    }

    // ===== getStreamInfo() testovi =====

    @Test
    void testGetStreamInfo_NormalVideo_ReturnsInfo() {
        // Given - obican video
        Map<String, Object> info = new HashMap<>();
        info.put("videoId", 1L);
        info.put("scheduled", false);
        info.put("available", true);
        info.put("scheduledAt", null);
        stubVideoService.setStreamInfo(info);

        // When
        ResponseEntity<?> response = videoController.getStreamInfo(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(1L, body.get("videoId"));
        assertFalse((Boolean) body.get("scheduled"));
        assertTrue((Boolean) body.get("available"));
    }

    @Test
    void testGetStreamInfo_ScheduledAvailable_HasOffset() {
        // Given - zakazan video koji je dostupan
        Map<String, Object> info = new HashMap<>();
        info.put("videoId", 1L);
        info.put("scheduled", true);
        info.put("available", true);
        info.put("streamOffsetSeconds", 1800L);
        stubVideoService.setStreamInfo(info);

        // When
        ResponseEntity<?> response = videoController.getStreamInfo(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(1800L, body.get("streamOffsetSeconds"));
        assertTrue((Boolean) body.get("scheduled"));
        assertTrue((Boolean) body.get("available"));
    }

    @Test
    void testGetStreamInfo_ScheduledNotAvailable_HasSecondsUntil() {
        // Given - zakazan video koji jos nije dostupan
        Map<String, Object> info = new HashMap<>();
        info.put("videoId", 1L);
        info.put("scheduled", true);
        info.put("available", false);
        info.put("secondsUntilAvailable", 3600L);
        stubVideoService.setStreamInfo(info);

        // When
        ResponseEntity<?> response = videoController.getStreamInfo(1L);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals(3600L, body.get("secondsUntilAvailable"));
        assertTrue((Boolean) body.get("scheduled"));
        assertFalse((Boolean) body.get("available"));
    }

    @Test
    void testGetStreamInfo_VideoNotFound_Returns404() {
        // Given
        stubVideoService.setThrowNotFound(true);

        // When
        ResponseEntity<?> response = videoController.getStreamInfo(999L);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    static class StubVideoService extends VideoService {

        private boolean videoAvailable = true;
        private byte[] videoBytes = new byte[0];
        private boolean throwNotFound = false;
        private boolean throwIOException = false;
        private Map<String, Object> streamInfo = new HashMap<>();

        public StubVideoService() {
            super(null, null, null, null, null, null, null, null, null);
        }

        public void setVideoAvailable(boolean available) {
            this.videoAvailable = available;
        }

        public void setVideoBytes(byte[] bytes) {
            this.videoBytes = bytes;
        }

        public void setThrowNotFound(boolean value) {
            this.throwNotFound = value;
        }

        public void setThrowIOException(boolean value) {
            this.throwIOException = value;
        }

        public void setStreamInfo(Map<String, Object> info) {
            this.streamInfo = info;
        }

        @Override
        public boolean isVideoAvailable(Long videoId) {
            if (throwNotFound) {
                throw new IllegalArgumentException("Video not found");
            }
            return videoAvailable;
        }

        @Override
        public byte[] getVideoFile(Long videoId) throws IOException {
            if (throwNotFound) {
                throw new IllegalArgumentException("Video not found");
            }
            if (throwIOException) {
                throw new IOException("Simulated IO error");
            }
            return videoBytes;
        }

        @Override
        public Map<String, Object> getStreamInfo(Long videoId) {
            if (throwNotFound) {
                throw new IllegalArgumentException("Video not found");
            }
            return streamInfo;
        }
    }
}
