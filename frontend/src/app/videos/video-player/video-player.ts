import { Component, OnInit, ChangeDetectorRef } from '@angular/core'; // DODAJ ChangeDetectorRef
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
export class VideoPlayerComponent implements OnInit {
  video: Video | null = null;
  videoStreamUrl: string = '';
  loading: boolean = true;
  error: string = '';
  viewCountIncremented: boolean = false;

  otherVideos: Video[] = [];

  comments: VideoComment[] = [];
  newComment: string = '';
  postingComment: boolean = false;

  
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

  loadVideo(videoId: number) {
    this.loading = true;
    this.error = '';
    this.cdr.detectChanges(); // DODAJ OVO - da se odmah pokaže loading

    this.videoService.getVideoById(videoId).subscribe({
      next: (video) => {
        this.video = video;
        this.videoStreamUrl = this.videoService.getVideoStreamUrl(videoId);
        this.loading = false;
        this.cdr.detectChanges(); // DODAJ OVO

        // Increment view count kada se video učita
        this.incrementViewCount(videoId);
      },
      error: (err) => {
        this.error = 'Video nije pronađen';
        this.loading = false;
        this.cdr.detectChanges(); // DODAJ OVO
      }
    });
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
          // Ažuriraj view count u UI-ju
          if (this.video) {
            this.video.viewCount += 1;
            this.cdr.detectChanges(); // DODAJ OVO - za update view count
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
}