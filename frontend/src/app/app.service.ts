import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AppService {

  private baseUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  login(data: { username: string; password: string }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/login`, data);
  }

  register(data: { username: string; password: string; role: string }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/register`, data);
  }
}
