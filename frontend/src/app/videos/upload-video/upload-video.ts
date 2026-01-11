import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { VideoService } from '../../../core/services/video.service';

@Component({
  selector: 'app-upload-video',
  templateUrl: './upload-video.html',
  styleUrls: ['./upload-video.css'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class UploadVideoComponent {
  title: string = '';
  description: string = '';
  tags: string = '';
  location: string = '';

  videoFile: File | null = null;
  thumbnailFile: File | null = null;

  videoFileName: string = '';
  thumbnailFileName: string = '';

  error: string = '';
  success: string = '';
  uploading: boolean = false;
  uploadProgress: number = 0;

  constructor(
    private videoService: VideoService,
    private router: Router
  ) {}

  onVideoFileSelected(event: any) {
    const file: File = event.target.files[0];
    
    if (file) {
      // Validacija tipa fajla
      if (file.type !== 'video/mp4') {
        this.error = 'Samo MP4 video format je dozvoljen';
        this.videoFile = null;
        this.videoFileName = '';
        return;
      }

      // Validacija veličine (200MB max)
      const maxSize = 200 * 1024 * 1024; // 200MB
      if (file.size > maxSize) {
        this.error = 'Video ne sme biti veći od 200MB';
        this.videoFile = null;
        this.videoFileName = '';
        return;
      }

      this.videoFile = file;
      this.videoFileName = file.name;
      this.error = '';
    }
  }

  onThumbnailFileSelected(event: any) {
    const file: File = event.target.files[0];
    
    if (file) {
      // Validacija tipa fajla
      if (!file.type.startsWith('image/')) {
        this.error = 'Samo slike su dozvoljene za thumbnail';
        this.thumbnailFile = null;
        this.thumbnailFileName = '';
        return;
      }

      this.thumbnailFile = file;
      this.thumbnailFileName = file.name;
      this.error = '';
    }
  }

  uploadVideo() {
    this.error = '';
    this.success = '';

    // Validacija obaveznih polja
    if (!this.title.trim()) {
      this.error = 'Naslov je obavezan';
      return;
    }

    if (!this.description.trim()) {
      this.error = 'Opis je obavezan';
      return;
    }

    if (!this.tags.trim()) {
      this.error = 'Tagovi su obavezni';
      return;
    }

    if (!this.videoFile) {
      this.error = 'Video fajl je obavezan';
      return;
    }

    if (!this.thumbnailFile) {
      this.error = 'Thumbnail slika je obavezna';
      return;
    }

    this.uploading = true;
    this.uploadProgress = 0;

    // Simulacija progress-a
    const progressInterval = setInterval(() => {
      if (this.uploadProgress < 90) {
        this.uploadProgress += 10;
      }
    }, 500);

    this.videoService.createVideo(
      this.title,
      this.description,
      this.tags,
      this.location || null,
      this.videoFile,
      this.thumbnailFile
    ).subscribe({
      next: (response) => {
        clearInterval(progressInterval);
        this.uploadProgress = 100;
        this.success = 'Video uspešno postavljen! 🎬';
        this.uploading = false;

        // Reset forme
        setTimeout(() => {
          this.resetForm();
          this.router.navigate(['/']);
        }, 2000);
      },
      error: (err) => {
        clearInterval(progressInterval);
        this.uploading = false;
        this.uploadProgress = 0;

        if (err.status === 400) {
          this.error = err.error || 'Neispravni podaci';
        } else if (err.status === 401) {
          this.error = 'Morate biti prijavljeni da biste postavili video';
        } else if (err.status === 413) {
          this.error = 'Fajl je prevelik';
        } else {
          this.error = 'Greška pri postavljanju videa. Pokušajte ponovo.';
        }
      }
    });
  }

  resetForm() {
    this.title = '';
    this.description = '';
    this.tags = '';
    this.location = '';
    this.videoFile = null;
    this.thumbnailFile = null;
    this.videoFileName = '';
    this.thumbnailFileName = '';
    this.error = '';
    this.success = '';
    this.uploadProgress = 0;
  }

  cancel() {
    this.router.navigate(['/']);
  }
}
