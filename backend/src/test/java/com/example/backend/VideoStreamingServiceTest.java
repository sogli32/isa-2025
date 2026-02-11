package com.example.backend;

import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.VideoLikeRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.repository.VideoViewRepository;
import com.example.backend.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoStreamingServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoLikeRepository videoLikeRepository;

    @Mock
    private VideoViewRepository videoViewRepository;

    @Mock
    private ThumbnailCacheService thumbnailCacheService;

    @Mock
    private PopularityCalculationService popularityCalculationService;

    @Mock
    private GeolocationService geolocationService;

    // Rucni stub za FileStorageService (Mockito ne moze klase na Java 25)
    private StubFileStorageService stubFileStorage;

    private VideoService videoService;

    private User testUser;

    @BeforeEach
    void setUp() {
        stubFileStorage = new StubFileStorageService();

        videoService = new VideoService(
                videoRepository,
                userRepository,
                videoLikeRepository,
                videoViewRepository,
                stubFileStorage,
                thumbnailCacheService,
                popularityCalculationService,
                geolocationService
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
    }

    // ===== isVideoAvailable() testovi =====

    @Test
    void testIsVideoAvailable_NoSchedule_ReturnsTrue() {
        // Given - video bez zakazivanja
        Video video = createVideo("Normal Video", null);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // When
        boolean available = videoService.isVideoAvailable(1L);

        // Then
        assertTrue(available);
    }

    @Test
    void testIsVideoAvailable_ScheduledInPast_ReturnsTrue() {
        // Given - video zakazan u proslosti (vec dostupan)
        Video video = createVideo("Past Scheduled", LocalDateTime.now().minusHours(2));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // When
        boolean available = videoService.isVideoAvailable(1L);

        // Then
        assertTrue(available);
    }

    @Test
    void testIsVideoAvailable_ScheduledInFuture_ReturnsFalse() {
        // Given - video zakazan za buducnost (jos nije dostupan)
        Video video = createVideo("Future Scheduled", LocalDateTime.now().plusHours(2));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // When
        boolean available = videoService.isVideoAvailable(1L);

        // Then
        assertFalse(available);
    }

    @Test
    void testIsVideoAvailable_VideoNotFound_ThrowsException() {
        // Given
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            videoService.isVideoAvailable(999L);
        });
    }

    // ===== getStreamInfo() testovi =====

    @Test
    void testGetStreamInfo_NormalVideo_NotScheduled() {
        // Given - obican video bez zakazivanja
        Video video = createVideo("Normal Video", null);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // When
        Map<String, Object> info = videoService.getStreamInfo(1L);

        // Then
        assertEquals(1L, info.get("videoId"));
        assertFalse((Boolean) info.get("scheduled"));
        assertTrue((Boolean) info.get("available"));
        assertNull(info.get("scheduledAt"));
        assertNull(info.get("streamOffsetSeconds"));
        assertNull(info.get("secondsUntilAvailable"));
    }

    @Test
    void testGetStreamInfo_ScheduledVideo_Available_HasOffset() {
        // Given - zakazan video koji je vec dostupan (scheduledAt u proslosti)
        LocalDateTime scheduledAt = LocalDateTime.now().minusMinutes(30);
        Video video = createVideo("Available Scheduled", scheduledAt);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // When
        Map<String, Object> info = videoService.getStreamInfo(1L);

        // Then
        assertEquals(1L, info.get("videoId"));
        assertTrue((Boolean) info.get("scheduled"));
        assertTrue((Boolean) info.get("available"));
        assertEquals(scheduledAt, info.get("scheduledAt"));

        // Offset treba biti oko 1800 sekundi (30 min)
        Long offset = (Long) info.get("streamOffsetSeconds");
        assertNotNull(offset);
        assertTrue(offset >= 1790 && offset <= 1810,
                "Offset bi trebao biti oko 1800s, ali je: " + offset);
        assertNull(info.get("secondsUntilAvailable"));
    }

    @Test
    void testGetStreamInfo_ScheduledVideo_NotYetAvailable_HasSecondsUntil() {
        // Given - zakazan video koji jos nije dostupan
        LocalDateTime scheduledAt = LocalDateTime.now().plusMinutes(60);
        Video video = createVideo("Future Scheduled", scheduledAt);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // When
        Map<String, Object> info = videoService.getStreamInfo(1L);

        // Then
        assertEquals(1L, info.get("videoId"));
        assertTrue((Boolean) info.get("scheduled"));
        assertFalse((Boolean) info.get("available"));
        assertEquals(scheduledAt, info.get("scheduledAt"));

        // secondsUntilAvailable treba biti oko 3600 sekundi (60 min)
        Long secondsUntil = (Long) info.get("secondsUntilAvailable");
        assertNotNull(secondsUntil);
        assertTrue(secondsUntil >= 3590 && secondsUntil <= 3610,
                "secondsUntilAvailable bi trebao biti oko 3600s, ali je: " + secondsUntil);
        assertNull(info.get("streamOffsetSeconds"));
    }

    @Test
    void testGetStreamInfo_VideoNotFound_ThrowsException() {
        // Given
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            videoService.getStreamInfo(999L);
        });
    }

    @Test
    void testGetStreamInfo_ScheduledJustNow_OffsetIsZeroOrSmall() {
        // Given - video zakazan upravo sada
        LocalDateTime scheduledAt = LocalDateTime.now();
        Video video = createVideo("Just Now", scheduledAt);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        // When
        Map<String, Object> info = videoService.getStreamInfo(1L);

        // Then
        assertTrue((Boolean) info.get("available"));
        Long offset = (Long) info.get("streamOffsetSeconds");
        assertNotNull(offset);
        assertTrue(offset >= 0 && offset <= 5,
                "Offset bi trebao biti 0 ili mali: " + offset);
    }

    // ===== getVideoFile() testovi =====

    @Test
    void testGetVideoFile_Success() throws IOException {
        // Given
        Video video = createVideo("Test Video", null);
        video.setVideoPath("test-video.mp4");
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        stubFileStorage.setVideoBytes(new byte[]{1, 2, 3, 4, 5});

        // When
        byte[] result = videoService.getVideoFile(1L);

        // Then
        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals("test-video.mp4", stubFileStorage.getLastLoadedFilename());
        assertTrue(stubFileStorage.wasLastLoadVideo());
    }

    @Test
    void testGetVideoFile_VideoNotFound_ThrowsException() {
        // Given
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            videoService.getVideoFile(999L);
        });
    }

    @Test
    void testGetVideoFile_FileIOException_Propagates() {
        // Given
        Video video = createVideo("Test Video", null);
        video.setVideoPath("missing-file.mp4");
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video));

        stubFileStorage.setThrowIOException(true);

        // When & Then
        assertThrows(IOException.class, () -> {
            videoService.getVideoFile(1L);
        });
    }

    // ===== Helper metode =====

    private Video createVideo(String title, LocalDateTime scheduledAt) {
        Video video = new Video(
                title, "Description", "tags",
                "thumb.jpg", "video.mp4",
                testUser, null
        );
        video.setId(1L);
        video.setViewCount(0L);
        if (scheduledAt != null) {
            video.setScheduledAt(scheduledAt);
        }
        return video;
    }

    // ===== Stub FileStorageService (zaobilazi Java 25 + ByteBuddy problem) =====

    static class StubFileStorageService extends FileStorageService {
        private byte[] videoBytes = new byte[0];
        private boolean throwIOException = false;
        private String lastLoadedFilename;
        private boolean lastLoadWasVideo;

        public StubFileStorageService() {
            // Ne poziva super() kreiranje direktorijuma
        }

        public void setVideoBytes(byte[] bytes) {
            this.videoBytes = bytes;
        }

        public void setThrowIOException(boolean value) {
            this.throwIOException = value;
        }

        public String getLastLoadedFilename() {
            return lastLoadedFilename;
        }

        public boolean wasLastLoadVideo() {
            return lastLoadWasVideo;
        }

        @Override
        public byte[] loadFile(String filename, boolean isVideo) throws IOException {
            this.lastLoadedFilename = filename;
            this.lastLoadWasVideo = isVideo;
            if (throwIOException) {
                throw new IOException("Simulated file not found");
            }
            return videoBytes;
        }
    }
}
