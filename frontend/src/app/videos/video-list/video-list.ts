import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { VideoService } from '../../../core/services/video.service';
import { Video } from '../../../core/models/video.model';
import { GeolocationService } from '../../../core/services/geolocation.service';

@Component({
  selector: 'app-video-list',
  templateUrl: './video-list.html',
  styleUrls: ['./video-list.css'],
  standalone: true,
  imports: [CommonModule, FormsModule] 
})
export class VideoListComponent implements OnInit {
  videos: Video[] = [];
  loading: boolean = true;
  error: string = '';
  activeTab: 'newest' | 'trending' | 'nearby' | 'popularNearby' = 'newest';

  selectedRadius: number = 10; 
  private radiusUpdateSubject = new Subject<number>();

  constructor(
    private videoService: VideoService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private geoService: GeolocationService
  ) {
   
    this.radiusUpdateSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(newRadius => {
      this.selectedRadius = newRadius;
      this.loadVideos();
    });
  }

  ngOnInit(): void {
    this.loadVideos();
  }

  onRadiusChange() {
    this.radiusUpdateSubject.next(this.selectedRadius);
  }

  setTab(tab: 'newest' | 'trending' | 'nearby' | 'popularNearby') {
    if (this.activeTab !== tab) {
      this.activeTab = tab;
      this.loadVideos();
    }
  }

  loadVideos() {
    this.loading = true;
    this.error = '';

    switch (this.activeTab) {
      case 'newest':
        this.videoService.getAllVideos().subscribe({
          next: (videos) => this.handleVideos(videos),
          error: () => this.handleError('Greška pri učitavanju najnovijih videa')
        });
        break;

      case 'trending':
        this.videoService.getTrendingVideos().subscribe({
          next: (videos) => this.handleVideos(videos),
          error: () => this.handleError('Greška pri učitavanju trending videa')
        });
        break;

      case 'nearby':
        this.loadNearbyVideos(this.selectedRadius, false);
        break;

      case 'popularNearby':
        this.loadNearbyVideos(this.selectedRadius, true);
        break;
    }
  }

  private loadNearbyVideos(radiusKm: number, popular: boolean) {
    this.geoService.getUserLocation().subscribe({
      next: (loc) => {
        if (loc.latitude && loc.longitude) {
          const request$ = popular
            ? this.videoService.getPopularVideosNearby(loc.latitude, loc.longitude, radiusKm)
            : this.videoService.getVideosNearby(loc.latitude, loc.longitude, radiusKm);

          request$.subscribe({
            next: (videos) => this.handleVideos(videos),
            error: () => this.handleError('Greška pri učitavanju videa u blizini')
          });
        } else {
          this.handleError('Ne može se odrediti vaša lokacija.');
        }
      },
      error: () => this.handleError('Greška pri dobijanju lokacije.')
    });
  }

  private handleVideos(videos: any[]) {
    this.videos = videos;
    this.loading = false;
    this.error = '';
    this.cdr.detectChanges();
  }

  private handleError(msg: string) {
    this.videos = [];
    this.loading = false;
    this.error = msg;
    this.cdr.detectChanges();
  }

  /**
   * Dobijanje URL-a za thumbnail
   */
  getThumbnailUrl(videoId: number): string {
    return this.videoService.getThumbnailUrl(videoId);
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

  /**
   * Otvaranje videa po ID-u
   */
  openVideo(videoId: number) {
    this.router.navigate(['/video', videoId]);
  }

  
  formatViewCount(count: number): string {
    if (count >= 1_000_000) return (count / 1_000_000).toFixed(1) + 'M';
    if (count >= 1_000) return (count / 1_000).toFixed(1) + 'K';
    return count.toString();
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Danas';
    if (diffDays === 1) return 'Juče';
    if (diffDays < 7) return `Pre ${diffDays} dana`;
    if (diffDays < 30) {
      const weeks = Math.floor(diffDays / 7);
      return `Pre ${weeks} ${weeks === 1 ? 'nedelje' : 'nedelja'}`;
    }
    if (diffDays < 365) {
      const months = Math.floor(diffDays / 30);
      return `Pre ${months} ${months === 1 ? 'mesec' : 'meseci'}`;
    }
    const years = Math.floor(diffDays / 365);
    return `Pre ${years} ${years === 1 ? 'godinu' : 'godina'}`;
  }
}
