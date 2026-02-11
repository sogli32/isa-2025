import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService } from '../../../core/services/video.service';
import { Video } from '../../../core/models/video.model';
import { VideoComment } from '../../../core/models/comment.model';
import { CommentService } from '../../../core/services/comment.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-video-player',
  templateUrl: './video-player.html',
  styleUrls: ['./video-player.css'],
  standalone: true,
  imports: [CommonModule,FormsModule]
})
export class VideoPlayerComponent implements OnInit, OnDestroy {
  video: Video | null = null;
  videoStreamUrl: string = '';
  loading: boolean = true;
  error: string = '';
  viewCountIncremented: boolean = false;

  otherVideos: Video[] = [];

  comments: VideoComment[] = [];
  newComment: string = '';
  postingComment: boolean = false;

  // Simulirani streaming za zakazane videe
  isScheduledStream: boolean = false;
  streamOffsetSeconds: number = 0;
  videoEnded: boolean = false;
  private seekSyncInterval: any = null;
  private countdownInterval: any = null;
  countdownText: string = '';
  isWaitingForSchedule: boolean = false;
  private isSyncingPosition: boolean = false;

  @ViewChild('videoElement') videoElementRef!: ElementRef<HTMLVideoElement>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService,
    public commentService: CommentService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit() {
  const videoId = Number(this.route.snapshot.paramMap.get('id'));
  if (!videoId || isNaN(videoId)) {
    this.error = 'Neispravan ID videa';
    this.loading = false;
    this.cdr.detectChanges();
    return;
  }

  this.loadVideo(videoId);
  this.loadOtherVideos(videoId);
  this.loadComments(videoId);
}

  ngOnDestroy() {
    if (this.seekSyncInterval) {
      clearInterval(this.seekSyncInterval);
    }
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  loadComments(videoId: number) {
    this.commentService.getComments(videoId).subscribe({
      next: (comments) => {
        this.comments = comments;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load comments', err)
    });
  }

  addComment() {
    if (!this.newComment.trim()) return;

    if (!this.video) return;
    this.postingComment = true;

    this.commentService.postComment(this.video.id, this.newComment).subscribe({
      next: (comment) => {
        this.comments.push(comment);
        this.newComment = '';
        this.postingComment = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to post comment', err);
        this.postingComment = false;
      }
    });
  }

  likeVideo() {
  if (!this.video) return;

  this.videoService.toggleLike(this.video.id).subscribe({
    next: (res) => {
      if (this.video) {
        this.video.likedByUser = res.liked;
        this.video.likeCount = res.likeCount;
        this.cdr.detectChanges();
      }
    },
    error: (err) => console.error('Failed to toggle like', err)
  });
}


  loadVideo(videoId: number) {
    this.loading = true;
    this.error = '';
    this.isScheduledStream = false;
    this.videoEnded = false;
    this.isWaitingForSchedule = false;
    this.isSyncingPosition = false;
    this.cdr.detectChanges();

    this.videoService.getVideoById(videoId).subscribe({
      next: (video) => {
        this.video = video;

        // Ako je zakazan i dostupan - simulirani streaming
        if (video.scheduledAt && video.available) {
          this.isScheduledStream = true;
          // Racunaj offset lokalno
          const now = new Date();
          const scheduled = new Date(video.scheduledAt);
          this.streamOffsetSeconds = Math.max(0, Math.floor((now.getTime() - scheduled.getTime()) / 1000));
          console.log('Stream detektovan. scheduledAt:', video.scheduledAt, 'lokalni offset:', this.streamOffsetSeconds, 'sekundi');
        }

        this.videoStreamUrl = this.videoService.getVideoStreamUrl(videoId);
        this.loading = false;
        this.cdr.detectChanges();

        // Increment view count kada se video ucita
        this.incrementViewCount(videoId);
      },
      error: (err) => {
        this.error = 'Video nije pronadjen';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
onVideoLoaded() {
    // Ako nije stream, samo pusti normalno
    if (!this.isScheduledStream || !this.video || !this.videoElementRef) {
      this.videoElementRef?.nativeElement.play().catch(() => {});
      return;
    }

    const videoEl = this.videoElementRef.nativeElement;

    console.log('=== STREAM LOADED ===');
    console.log('scheduledAt:', this.video.scheduledAt);
    console.log('duration:', videoEl.duration);
    console.log('readyState:', videoEl.readyState);

    // Sacekaj da video bude zaista spreman za seek
    const applyOffset = () => {
      const now = new Date();
      const scheduled = new Date(this.video!.scheduledAt!);
      const offsetSeconds = Math.floor((now.getTime() - scheduled.getTime()) / 1000);

      console.log('Primenjujem offset:', offsetSeconds, 'sek, duration:', videoEl.duration);

      this.isSyncingPosition = true;

      if (offsetSeconds < 0) {
        videoEl.currentTime = 0;
      } else if (videoEl.duration && offsetSeconds >= videoEl.duration) {
        this.videoEnded = true;
        videoEl.currentTime = videoEl.duration;
        this.cdr.detectChanges();
      } else {
        videoEl.currentTime = offsetSeconds;
      }

      // Kada browser zavrsi seek, pokreni play
      const onSeekedOnce = () => {
        videoEl.removeEventListener('seeked', onSeekedOnce);
        this.isSyncingPosition = false;
        console.log('Seek zavrsen, currentTime:', videoEl.currentTime);

        if (!this.videoEnded) {
          videoEl.play().catch(err => {
            console.warn('Autoplay blokiran:', err);
          });
        }
      };
      videoEl.addEventListener('seeked', onSeekedOnce);

      // Fallback ako seeked ne puca (npr. offset = 0)
      setTimeout(() => {
        this.isSyncingPosition = false;
      }, 500);

      this.startSeekSync(videoEl);
    };

    // Ako video vec ima dovoljno podataka, primeni odmah
    if (videoEl.readyState >= 2 && videoEl.duration > 0) {
      applyOffset();
    } else {
      // Sacekaj canplay event
      const onCanPlay = () => {
        videoEl.removeEventListener('canplay', onCanPlay);
        applyOffset();
      };
      videoEl.addEventListener('canplay', onCanPlay);
    }
  }

  private startSeekSync(videoEl: HTMLVideoElement) {
    if (this.seekSyncInterval) clearInterval(this.seekSyncInterval);

    this.seekSyncInterval = setInterval(() => {
      if (!this.video || !this.video.scheduledAt || this.videoEnded || this.isSyncingPosition) return;

      const now = new Date();
      const scheduled = new Date(this.video.scheduledAt!);
      const livePosition = Math.floor((now.getTime() - scheduled.getTime()) / 1000);

      // Provera kraja
      if (livePosition >= videoEl.duration) {
        this.videoEnded = true;
        videoEl.pause();
        clearInterval(this.seekSyncInterval);
        this.cdr.detectChanges();
        return;
      }

      // Samo spreci da currentTime ode UNAPRED od live pozicije
      // Unazad je dozvoljeno - korisnik moze premotavati
      if (videoEl.currentTime > livePosition + 2) {
        this.isSyncingPosition = true;
        videoEl.currentTime = livePosition;
        setTimeout(() => { this.isSyncingPosition = false; }, 200);
      }
    }, 2000);
  }

  onVideoSeeked() {
    // Ako je programski seek (iz onVideoLoaded ili startSeekSync), ignorisi
    if (this.isSyncingPosition) return;
    if (!this.isScheduledStream || !this.video) return;

    const videoEl = this.videoElementRef.nativeElement;
    const now = new Date();
    const scheduled = new Date(this.video.scheduledAt!);
    const livePosition = Math.floor((now.getTime() - scheduled.getTime()) / 1000);

    // Dozvoli premotavanje UNAZAD (ali ne unapred od live pozicije)
    if (videoEl.currentTime > livePosition + 2) {
      this.isSyncingPosition = true;
      videoEl.currentTime = livePosition;
      setTimeout(() => { this.isSyncingPosition = false; }, 200);
    }
    // Premotavanje unazad je dozvoljeno - ne radi nista
  }

  private startCountdown(scheduledAtStr: string) {
    const updateCountdown = () => {
      const scheduledAt = new Date(scheduledAtStr);
      const now = new Date();
      const diffMs = scheduledAt.getTime() - now.getTime();

      if (diffMs <= 0) {
        this.isWaitingForSchedule = false;
        clearInterval(this.countdownInterval);
        // Ponovo ucitaj video - sada bi trebao biti dostupan
        if (this.video) {
          this.loadVideo(this.video.id);
        }
        return;
      }

      const hours = Math.floor(diffMs / (1000 * 60 * 60));
      const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diffMs % (1000 * 60)) / 1000);

      this.countdownText = `${this.pad(hours)}:${this.pad(minutes)}:${this.pad(seconds)}`;
      this.cdr.detectChanges();
    };

    updateCountdown();
    this.countdownInterval = setInterval(updateCountdown, 1000);
  }

  private pad(num: number): string {
    return num.toString().padStart(2, '0');
  }

  loadOtherVideos(currentVideoId: number) {
    this.videoService.getAllVideos().subscribe({
        next: (videos) => {
            // izbaci trenutni video iz liste
            this.otherVideos = videos.filter(v => v.id !== currentVideoId);
            this.cdr.detectChanges();
        },
        error: (err) => console.error('Failed to load other videos', err)
    });
  }
  getThumbnailUrl(videoId: number): string {
  return this.videoService.getThumbnailUrl(videoId);
}

  incrementViewCount(videoId: number) {
    if (!this.viewCountIncremented) {
      this.videoService.incrementViewCount(videoId).subscribe({
        next: () => {
          this.viewCountIncremented = true;
          // Azuriraj view count u UI-ju
          if (this.video) {
            this.video.viewCount += 1;
            this.cdr.detectChanges();
          }
        },
        error: (err) => {
          console.error('Failed to increment view count', err);
        }
      });
    }
  }

  goToVideo(videoId: number) {
    this.router.navigate(['/video', videoId]).then(() => {
        this.loadVideo(videoId);
        this.loadOtherVideos(videoId);
    });
  }

 goBack() {
    this.router.navigate(['/']);
  }

  formatViewCount(count: number): string {
    if (count >= 1000000) {
      return (count / 1000000).toFixed(1) + 'M';
    } else if (count >= 1000) {
      return (count / 1000).toFixed(1) + 'K';
    }
    return count.toString();
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('sr-RS', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  }

  formatScheduledDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('sr-RS', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    }) + ' u ' + date.toLocaleTimeString('sr-RS', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
