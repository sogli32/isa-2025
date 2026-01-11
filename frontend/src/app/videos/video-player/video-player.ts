import { Component, OnInit, ChangeDetectorRef } from '@angular/core'; // DODAJ ChangeDetectorRef
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { VideoService } from '../../../core/services/video.service';
import { Video } from '../../../core/models/video.model';

@Component({
  selector: 'app-video-player',
  templateUrl: './video-player.html',
  styleUrls: ['./video-player.css'],
  standalone: true,
  imports: [CommonModule]
})
export class VideoPlayerComponent implements OnInit {
  video: Video | null = null;
  videoStreamUrl: string = '';
  loading: boolean = true;
  error: string = '';
  viewCountIncremented: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private videoService: VideoService,
    private cdr: ChangeDetectorRef // DODAJ OVO
  ) {}

  ngOnInit() {
    const videoId = Number(this.route.snapshot.paramMap.get('id'));
    
    if (!videoId || isNaN(videoId)) {
      this.error = 'Neispravan ID videa';
      this.loading = false;
      this.cdr.detectChanges(); // DODAJ OVO
      return;
    }

    this.loadVideo(videoId);
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