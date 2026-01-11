import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';  // ← DODAJ tap

@Injectable({ providedIn: 'root' })
export class AppService {

  private baseUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient) {}

  login(data: { email: string; password: string }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/login`, data).pipe(
      tap(response => {
        // KRITIČNO: Čuvanje tokena u localStorage
        localStorage.setItem('token', response.token);
        localStorage.setItem('username', response.username);
        localStorage.setItem('role', response.role);
        console.log('✅ Token saved to localStorage:', response.token);
      })
    );
  }

  register(data: { username: string; email: string; password: string; confirmPassword: string; firstName: string; lastName: string; address: string; role: string }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/register`, data);
  }
  
  // DODAJ ove metode:
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isLoggedIn(): boolean {
    return this.getToken() !== null;
  }
}