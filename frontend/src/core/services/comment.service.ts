import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { VideoComment } from '../models/comment.model';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class CommentService {
  private apiUrl = 'http://localhost:8080/api/comments';

  constructor(private http: HttpClient, public authService: AuthService) {}

  // Dobijanje komentara za video
  getComments(videoId: number): Observable<VideoComment[]> {
    return this.http.get<VideoComment[]>(`${this.apiUrl}/${videoId}`);
  }

  // Slanje novog komentara (samo ulogovani)
  postComment(videoId: number, content: string): Observable<VideoComment> {
    const token = this.authService.getToken(); // pretpostavljamo da AuthService ima getToken
    return this.http.post<VideoComment>(
      `${this.apiUrl}/${videoId}`,
      { content },
      {
        headers: { Authorization: `Bearer ${token}` }
      }
    );
  }
}
