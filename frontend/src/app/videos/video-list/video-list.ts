import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { VideoService } from '../../../core/services/video.service';
import { Video } from '../../../core/models/video.model';

@Component({
  selector: 'app-video-list',
  templateUrl: './video-list.html',
  styleUrls: ['./video-list.css'],
  standalone: true,
  imports: [CommonModule]
})
export class VideoListComponent implements OnInit {
  videos: Video[] = [];
  loading: boolean = true;
  error: string = '';

  // Aktivni tab: 'newest' ili 'trending'
  activeTab: 'newest' | 'trending' = 'newest';

  constructor(
    private videoService: VideoService,
    private router: Router,
    private cdr: ChangeDetectorRef 
  ) {}

  ngOnInit() {
    this.loadVideos();
  }

  // Promena taba
  setTab(tab: 'newest' | 'trending') {
    if (this.activeTab !== tab) {
      this.activeTab = tab;
      this.loadVideos();
    }
  }

  // Učitavanje videa u zavisnosti od taba
  loadVideos() {
    this.loading = true;
    this.error = '';

    const fetch$ = this.activeTab === 'newest'
      ? this.videoService.getAllVideos()
      : this.videoService.getTrendingVideos();

    fetch$.subscribe({
      next: (videos) => {
        this.videos = videos;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Greška pri učitavanju videa';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  getThumbnailUrl(videoId: number): string {
    return this.videoService.getThumbnailUrl(videoId);
  }

  openVideo(videoId: number) {
    this.router.navigate(['/video', videoId]);
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
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
      return 'Danas';
    } else if (diffDays === 1) {
      return 'Juče';
    } else if (diffDays < 7) {
      return `Pre ${diffDays} dana`;
    } else if (diffDays < 30) {
      const weeks = Math.floor(diffDays / 7);
      return `Pre ${weeks} ${weeks === 1 ? 'nedelje' : 'nedelja'}`;
    } else if (diffDays < 365) {
      const months = Math.floor(diffDays / 30);
      return `Pre ${months} ${months === 1 ? 'mesec' : 'meseci'}`;
    } else {
      const years = Math.floor(diffDays / 365);
      return `Pre ${years} ${years === 1 ? 'godinu' : 'godina'}`;
    }
  }
}
