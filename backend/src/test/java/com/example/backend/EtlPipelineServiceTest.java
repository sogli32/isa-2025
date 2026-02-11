package com.example.backend;

import com.example.backend.model.EtlPipelineResult;
import com.example.backend.model.User;
import com.example.backend.model.Video;
import com.example.backend.repository.EtlPipelineResultRepository;
import com.example.backend.repository.VideoRepository;
import com.example.backend.repository.VideoViewRepository;
import com.example.backend.services.EtlPipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EtlPipelineServiceTest {

    @Mock
    private VideoViewRepository videoViewRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private EtlPipelineResultRepository etlPipelineResultRepository;

    @InjectMocks
    private EtlPipelineService etlPipelineService;

    private User testUser;
    private Video video1;
    private Video video2;
    private Video video3;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");

        video1 = new Video();
        video1.setId(1L);
        video1.setTitle("Video 1");
        video1.setUser(testUser);
        video1.setViewCount(10L);

        video2 = new Video();
        video2.setId(2L);
        video2.setTitle("Video 2");
        video2.setUser(testUser);
        video2.setViewCount(20L);

        video3 = new Video();
        video3.setId(3L);
        video3.setTitle("Video 3");
        video3.setUser(testUser);
        video3.setViewCount(30L);
    }

    @Test
    void testRunEtlPipeline_NoViews_SavesEmptyResult() {
        // Given - nema pregleda u poslednjih 7 dana
        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        etlPipelineService.runEtlPipeline();

        // Then - rezultat se cuva ali bez videa
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository, times(1)).save(captor.capture());

        EtlPipelineResult saved = captor.getValue();
        assertNotNull(saved.getExecutedAt());
        assertNull(saved.getVideo1());
        assertNull(saved.getVideo2());
        assertNull(saved.getVideo3());
        assertNull(saved.getScore1());
        assertNull(saved.getScore2());
        assertNull(saved.getScore3());
    }

    @Test
    void testRunEtlPipeline_SingleVideo_SavesAsTop1() {
        // Given - jedan video sa pregledima danas
        LocalDate today = LocalDate.now();
        List<Object[]> viewData = new ArrayList<>();
        viewData.add(new Object[]{1L, today, 5L}); // videoId=1, danas, 5 pregleda

        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(viewData);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video1));

        // When
        etlPipelineService.runEtlPipeline();

        // Then
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());

        EtlPipelineResult saved = captor.getValue();
        assertNotNull(saved.getVideo1());
        assertEquals(1L, saved.getVideo1().getId());
        // 5 pregleda * tezina 7 (danas) = 35.0
        assertEquals(35.0, saved.getScore1(), 0.01);
        assertNull(saved.getVideo2());
        assertNull(saved.getVideo3());
    }

    @Test
    void testRunEtlPipeline_WeightCalculation_TodayWeight7() {
        // Given - pregledi danas trebaju imati tezinu 7
        LocalDate today = LocalDate.now();
        List<Object[]> viewData = new ArrayList<>();
        viewData.add(new Object[]{1L, today, 10L}); // 10 pregleda danas

        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(viewData);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video1));

        // When
        etlPipelineService.runEtlPipeline();

        // Then - 10 * 7 = 70
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());
        assertEquals(70.0, captor.getValue().getScore1(), 0.01);
    }

    @Test
    void testRunEtlPipeline_WeightCalculation_YesterdayWeight7() {
        // Given - pregledi od juce trebaju imati tezinu 7 (8 - 1 = 7)
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Object[]> viewData = new ArrayList<>();
        viewData.add(new Object[]{1L, yesterday, 10L});

        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(viewData);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video1));

        // When
        etlPipelineService.runEtlPipeline();

        // Then - 10 * 7 (8-1) = 70
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());
        assertEquals(70.0, captor.getValue().getScore1(), 0.01);
    }

    @Test
    void testRunEtlPipeline_WeightCalculation_6DaysAgoWeight2() {
        // Given - pregledi od pre 6 dana trebaju imati tezinu 2 (8 - 6 = 2)
        LocalDate sixDaysAgo = LocalDate.now().minusDays(6);
        List<Object[]> viewData = new ArrayList<>();
        viewData.add(new Object[]{1L, sixDaysAgo, 10L});

        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(viewData);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video1));

        // When
        etlPipelineService.runEtlPipeline();

        // Then - 10 * 2 (8-6) = 20
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());
        assertEquals(20.0, captor.getValue().getScore1(), 0.01);
    }

    @Test
    void testRunEtlPipeline_Top3Selection_CorrectOrder() {
        // Given - 4 videa, trebaju se sacuvati samo top 3
        LocalDate today = LocalDate.now();
        List<Object[]> viewData = new ArrayList<>();
        viewData.add(new Object[]{1L, today, 2L});  // video1: 2*7 = 14
        viewData.add(new Object[]{2L, today, 5L});  // video2: 5*7 = 35
        viewData.add(new Object[]{3L, today, 10L}); // video3: 10*7 = 70
        viewData.add(new Object[]{4L, today, 1L});  // video4: 1*7 = 7 (ne ulazi u top 3)

        Video video4 = new Video();
        video4.setId(4L);
        video4.setTitle("Video 4");
        video4.setUser(testUser);

        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(viewData);
        when(videoRepository.findById(3L)).thenReturn(Optional.of(video3));
        when(videoRepository.findById(2L)).thenReturn(Optional.of(video2));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video1));

        // When
        etlPipelineService.runEtlPipeline();

        // Then - top 3 su video3 (70), video2 (35), video1 (14)
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());

        EtlPipelineResult saved = captor.getValue();
        assertEquals(3L, saved.getVideo1().getId()); // #1: video3 sa score 70
        assertEquals(70.0, saved.getScore1(), 0.01);
        assertEquals(2L, saved.getVideo2().getId()); // #2: video2 sa score 35
        assertEquals(35.0, saved.getScore2(), 0.01);
        assertEquals(1L, saved.getVideo3().getId()); // #3: video1 sa score 14
        assertEquals(14.0, saved.getScore3(), 0.01);
    }

    @Test
    void testRunEtlPipeline_MultiDayViews_CumulativeScore() {
        // Given - jedan video sa pregledima tokom vise dana
        LocalDate today = LocalDate.now();
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);

        List<Object[]> viewData = new ArrayList<>();
        viewData.add(new Object[]{1L, today, 3L});      // 3 * 7 = 21
        viewData.add(new Object[]{1L, yesterday, 5L});   // 5 * 7 (8-1) = 35
        viewData.add(new Object[]{1L, twoDaysAgo, 2L});  // 2 * 6 (8-2) = 12

        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(viewData);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video1));

        // When
        etlPipelineService.runEtlPipeline();

        // Then - ukupno: 21 + 35 + 12 = 68
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());
        assertEquals(68.0, captor.getValue().getScore1(), 0.01);
    }

    @Test
    void testRunEtlPipeline_TwoVideos_SavesOnlyTwo() {
        // Given - samo 2 videa
        LocalDate today = LocalDate.now();
        List<Object[]> viewData = new ArrayList<>();
        viewData.add(new Object[]{1L, today, 5L});  // video1: 35
        viewData.add(new Object[]{2L, today, 10L}); // video2: 70

        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(viewData);
        when(videoRepository.findById(2L)).thenReturn(Optional.of(video2));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video1));

        // When
        etlPipelineService.runEtlPipeline();

        // Then
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());

        EtlPipelineResult saved = captor.getValue();
        assertNotNull(saved.getVideo1()); // #1: video2
        assertEquals(2L, saved.getVideo1().getId());
        assertNotNull(saved.getVideo2()); // #2: video1
        assertEquals(1L, saved.getVideo2().getId());
        assertNull(saved.getVideo3());    // nema treceg
    }

    @Test
    void testRunEtlPipeline_RecentViewsWeighMoreThanOld() {
        // Given - video1 ima stare preglede, video2 ima nove preglede
        LocalDate today = LocalDate.now();
        LocalDate sixDaysAgo = LocalDate.now().minusDays(6);

        List<Object[]> viewData = new ArrayList<>();
        viewData.add(new Object[]{1L, sixDaysAgo, 10L}); // video1: 10 * 2 (8-6) = 20
        viewData.add(new Object[]{2L, today, 5L});        // video2: 5 * 7 = 35

        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(viewData);
        when(videoRepository.findById(2L)).thenReturn(Optional.of(video2));
        when(videoRepository.findById(1L)).thenReturn(Optional.of(video1));

        // When
        etlPipelineService.runEtlPipeline();

        // Then - video2 (35) je ispred video1 (20) iako video1 ima vise pregleda
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());

        EtlPipelineResult saved = captor.getValue();
        assertEquals(2L, saved.getVideo1().getId()); // video2 je #1
        assertEquals(35.0, saved.getScore1(), 0.01);
        assertEquals(1L, saved.getVideo2().getId()); // video1 je #2
        assertEquals(20.0, saved.getScore2(), 0.01);
    }

    @Test
    void testRunEtlPipeline_ExecutedAtIsSetCorrectly() {
        // Given
        when(videoViewRepository.countViewsPerVideoPerDay(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        LocalDateTime before = LocalDateTime.now();

        // When
        etlPipelineService.runEtlPipeline();

        LocalDateTime after = LocalDateTime.now();

        // Then
        ArgumentCaptor<EtlPipelineResult> captor = ArgumentCaptor.forClass(EtlPipelineResult.class);
        verify(etlPipelineResultRepository).save(captor.capture());

        LocalDateTime executedAt = captor.getValue().getExecutedAt();
        assertNotNull(executedAt);
        assertFalse(executedAt.isBefore(before));
        assertFalse(executedAt.isAfter(after));
    }
}
