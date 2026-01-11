import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Video } from '../models/video.model';

@Injectable({
  providedIn: 'root'
})
export class VideoService {

  private apiUrl = 'http://localhost:8080/api/videos';

  constructor(private http: HttpClient) {}

  /**
   * Kreiranje nove video objave
   */
  createVideo(
    title: string,
    description: string,
    tags: string,
    location: string | null,
    videoFile: File,
    thumbnailFile: File
  ): Observable<Video> {
    const formData = new FormData();
    formData.append('title', title);
    formData.append('description', description);
    formData.append('tags', tags);
    if (location) {
      formData.append('location', location);
    }
    formData.append('videoFile', videoFile);
    formData.append('thumbnailFile', thumbnailFile);

    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.post<Video>(this.apiUrl, formData, { headers });
  }

  /**
   * Lista svih videa
   */
  getAllVideos(): Observable<Video[]> {
    return this.http.get<Video[]>(this.apiUrl);
  }

  /**
   * Detalji videa po ID-u
   */
  getVideoById(id: number): Observable<Video> {
    return this.http.get<Video>(`${this.apiUrl}/${id}`);
  }

  /**
   * Increment view count
   */
  incrementViewCount(id: number): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/view`, {});
  }

  /**
   * URL za thumbnail sliku
   */
  getThumbnailUrl(id: number): string {
    return `${this.apiUrl}/${id}/thumbnail`;
  }

  /**
   * URL za video stream
   */
  getVideoStreamUrl(id: number): string {
    return `${this.apiUrl}/${id}/stream`;
  }

toggleLike(videoId: number) {
  const token = localStorage.getItem('token'); // uzmi token direktno
  const headers = new HttpHeaders({ 'Authorization': `Bearer ${token}` });

  return this.http.post<{ liked: boolean; likeCount: number }>(
    `${this.apiUrl}/${videoId}/like`,
    {},
    { headers }
  );
}


  deleteVideo(id: number): Observable<any> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.delete(`${this.apiUrl}/${id}`, { headers });
  }
}
