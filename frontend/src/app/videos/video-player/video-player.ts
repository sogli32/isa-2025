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
    this.cdr.detectChanges();

    this.videoService.getVideoById(videoId).subscribe({
      next: (video) => {
        this.video = video;

        // Proveri da li je zakazan video koji jos nije dostupan
        if (video.scheduledAt && !video.available) {
          this.isWaitingForSchedule = true;
          this.loading = false;
          this.startCountdown(video.scheduledAt);
          this.cdr.detectChanges();
          return;
        }

        // Ako je zakazan i dostupan - simulirani streaming
        if (video.scheduledAt && video.available && video.streamOffsetSeconds !== undefined && video.streamOffsetSeconds !== null) {
          this.isScheduledStream = true;
          this.streamOffsetSeconds = video.streamOffsetSeconds;
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
    if (this.isScheduledStream && this.videoElementRef) {
      const videoEl = this.videoElementRef.nativeElement;

      // Ponovo izracunaj offset sa servera za tacnost
      if (this.video) {
        this.videoService.getStreamInfo(this.video.id).subscribe({
          next: (info) => {
            if (info.streamOffsetSeconds !== undefined && info.streamOffsetSeconds !== null) {
              this.streamOffsetSeconds = info.streamOffsetSeconds;

              if (this.streamOffsetSeconds >= videoEl.duration) {
                this.videoEnded = true;
                videoEl.currentTime = videoEl.duration;
                videoEl.pause();
                this.cdr.detectChanges();
                return;
              }

              videoEl.currentTime = this.streamOffsetSeconds;
              videoEl.play();
              this.startSeekSync(videoEl);
            }
          }
        });
      }
    }
  }

  private startSeekSync(videoEl: HTMLVideoElement) {
    // Svakih 5 sekundi, proveri da li se korisnik vratio nazad - ako jeste, vrati ga na pravu poziciju
    this.seekSyncInterval = setInterval(() => {
      if (!this.video || !this.video.scheduledAt) return;

      this.videoService.getStreamInfo(this.video.id).subscribe({
        next: (info) => {
          if (info.streamOffsetSeconds !== undefined && info.streamOffsetSeconds !== null) {
            const serverOffset = info.streamOffsetSeconds;

            if (serverOffset >= videoEl.duration) {
              this.videoEnded = true;
              videoEl.pause();
              videoEl.currentTime = videoEl.duration;
              clearInterval(this.seekSyncInterval);
              this.cdr.detectChanges();
              return;
            }

            // Ako korisnik pokusa da premota, vrati ga na pravu poziciju
            const drift = Math.abs(videoEl.currentTime - serverOffset);
            if (drift > 3) {
              videoEl.currentTime = serverOffset;
            }
          }
        }
      });
    }, 5000);
  }

  onVideoSeeked() {
    if (!this.isScheduledStream || !this.video || !this.videoElementRef) return;
    const videoEl = this.videoElementRef.nativeElement;

    // Spreci korisnika da premota zakazani video
    this.videoService.getStreamInfo(this.video.id).subscribe({
      next: (info) => {
        if (info.streamOffsetSeconds !== undefined && info.streamOffsetSeconds !== null) {
          const serverOffset = info.streamOffsetSeconds;
          const drift = Math.abs(videoEl.currentTime - serverOffset);
          if (drift > 2) {
            videoEl.currentTime = serverOffset;
          }
        }
      }
    });
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
