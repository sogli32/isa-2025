import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { VideoService } from '../../../core/services/video.service';
import { Video } from '../../../core/models/video.model';
import { GeolocationService } from '../../../core/services/geolocation.service';

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

  // Aktivni tab
  activeTab: 'newest' | 'trending' | 'nearby' | 'popularNearby' = 'newest';

  constructor(
    private videoService: VideoService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private geoService: GeolocationService
  ) {}

  ngOnInit(): void {
    this.loadVideos();
  }

  /**
   * Promena taba
   */
  setTab(tab: 'newest' | 'trending' | 'nearby' | 'popularNearby') {
    if (this.activeTab !== tab) {
      this.activeTab = tab;
      this.loadVideos();
    }
  }

  /**
   * Učitavanje videa u zavisnosti od taba
   */
  loadVideos(radiusKm: number = 10) {
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
        this.loadNearbyVideos(radiusKm, false);
        break;

      case 'popularNearby':
        this.loadNearbyVideos(radiusKm, true);
        break;
    }
  }

  /**
   * Učitavanje videa u blizini (obično radius 10 km)
   */
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

  /**
   * Obrada uspešno dobijenih videa
   */
  private handleVideos(videos: Video[]) {
    this.videos = videos;
    this.loading = false;
    this.error = '';
    this.cdr.detectChanges();
  }

  /**
   * Obrada greške
   */
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

  /**
   * Otvaranje videa po ID-u
   */
  openVideo(videoId: number) {
    this.router.navigate(['/video', videoId]);
  }

  /**
   * Formatiranje broja pregleda
   */
  formatViewCount(count: number): string {
    if (count >= 1_000_000) return (count / 1_000_000).toFixed(1) + 'M';
    if (count >= 1_000) return (count / 1_000).toFixed(1) + 'K';
    return count.toString();
  }

  /**
   * Formatiranje datuma
   */
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
