import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService } from '../../../core/services/video.service';
import { Video } from '../../../core/models/video.model';
import { VideoComment } from '../../../core/models/comment.model';
import { CommentService } from '../../../core/services/comment.service';
import { ChatService, ChatMessage } from '../../../core/services/chat.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-video-player',
  templateUrl: './video-player.html',
  styleUrls: ['./video-player.css'],
  standalone: true,
  imports: [CommonModule, FormsModule]
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

  // ============ WEBSOCKET ČET ============
  chatMessages: ChatMessage[] = [];
  newChatMessage: string = '';
  chatConnected: boolean = false;
  currentUsername: string = '';

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
  @ViewChild('chatMessagesContainer') chatMessagesContainer!: ElementRef<HTMLDivElement>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService,
    public commentService: CommentService,
    private chatService: ChatService,
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

    // Povuci username iz localStorage (JWT decode bi bio bolji pristup)
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.currentUsername = payload.sub || 'Anonymous';
      } catch (e) {
        this.currentUsername = 'Anonymous';
      }
    } else {
      this.currentUsername = 'Anonymous';
    }

    // Konektuj se na WebSocket čet
    this.connectToChat(videoId);
  }

  ngOnDestroy() {
    if (this.seekSyncInterval) {
      clearInterval(this.seekSyncInterval);
    }
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
    // Diskonektuj se sa WebSocket-a
    this.chatService.disconnect();
  }

  // ============ WEBSOCKET ČET METODE ============

  connectToChat(videoId: number) {
    this.chatService.connect(videoId, this.currentUsername).subscribe({
      next: (message: ChatMessage) => {
        this.chatMessages.push(message);
        this.chatConnected = true;
        this.cdr.detectChanges();
        
        // Auto-scroll do dna četa
        setTimeout(() => this.scrollChatToBottom(), 100);
      },
      error: (err) => {
        console.error('Chat connection error:', err);
        this.chatConnected = false;
      }
    });
  }

  sendChatMessage() {
    if (!this.newChatMessage.trim() || !this.video) return;

    this.chatService.sendMessage(
      this.video.id,
      this.newChatMessage,
      this.currentUsername
    );

    this.newChatMessage = '';
  }

  scrollChatToBottom() {
    if (this.chatMessagesContainer) {
      const container = this.chatMessagesContainer.nativeElement;
      container.scrollTop = container.scrollHeight;
    }
  }

  getChatMessageClass(message: ChatMessage): string {
    if (message.type === 'JOIN') return 'chat-join';
    if (message.type === 'LEAVE') return 'chat-leave';
    if (message.sender === this.currentUsername) return 'chat-own';
    return 'chat-other';
  }

  formatChatTime(timestamp: string): string {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('sr-RS', { hour: '2-digit', minute: '2-digit' });
  }

  // ============ OSTALE METODE (bez promene) ============

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

        if (video.scheduledAt && video.available) {
          this.isScheduledStream = true;
          const now = new Date();
          const scheduled = new Date(video.scheduledAt);
          this.streamOffsetSeconds = Math.max(0, Math.floor((now.getTime() - scheduled.getTime()) / 1000));
          console.log('Stream detektovan. scheduledAt:', video.scheduledAt, 'lokalni offset:', this.streamOffsetSeconds, 'sekundi');
        }

        this.videoStreamUrl = this.videoService.getVideoStreamUrl(videoId);
        this.loading = false;
        this.cdr.detectChanges();

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
    if (!this.isScheduledStream || !this.video || !this.videoElementRef) {
      this.videoElementRef?.nativeElement.play().catch(() => {});
      return;
    }

    const videoEl = this.videoElementRef.nativeElement;

    console.log('=== STREAM LOADED ===');
    console.log('scheduledAt:', this.video.scheduledAt);
    console.log('duration:', videoEl.duration);
    console.log('readyState:', videoEl.readyState);

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

      setTimeout(() => {
        this.isSyncingPosition = false;
      }, 500);

      this.startSeekSync(videoEl);
    };

    if (videoEl.readyState >= 2 && videoEl.duration > 0) {
      applyOffset();
    } else {
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

      if (livePosition >= videoEl.duration) {
        this.videoEnded = true;
        videoEl.pause();
        clearInterval(this.seekSyncInterval);
        this.cdr.detectChanges();
        return;
      }

      if (videoEl.currentTime > livePosition + 2) {
        this.isSyncingPosition = true;
        videoEl.currentTime = livePosition;
        setTimeout(() => { this.isSyncingPosition = false; }, 200);
      }
    }, 2000);
  }

  onVideoSeeked() {
    if (this.isSyncingPosition) return;
    if (!this.isScheduledStream || !this.video) return;

    const videoEl = this.videoElementRef.nativeElement;
    const now = new Date();
    const scheduled = new Date(this.video.scheduledAt!);
    const livePosition = Math.floor((now.getTime() - scheduled.getTime()) / 1000);

    if (videoEl.currentTime > livePosition + 2) {
      this.isSyncingPosition = true;
      videoEl.currentTime = livePosition;
      setTimeout(() => { this.isSyncingPosition = false; }, 200);
    }
  }

  private startCountdown(scheduledAtStr: string) {
    const updateCountdown = () => {
      const scheduledAt = new Date(scheduledAtStr);
      const now = new Date();
      const diffMs = scheduledAt.getTime() - now.getTime();

      if (diffMs <= 0) {
        this.isWaitingForSchedule = false;
        clearInterval(this.countdownInterval);
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
    // Diskonektuj se sa trenutnog četa
    this.chatService.disconnect();
    this.chatMessages = [];
    
    this.router.navigate(['/video', videoId]).then(() => {
      this.loadVideo(videoId);
      this.loadOtherVideos(videoId);
      this.connectToChat(videoId);
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
