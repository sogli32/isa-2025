import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { User } from '../models/user.model';
import { tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private apiUrl = 'http://localhost:8080/api/auth';

  user = signal<User | null>(null);

  constructor(private http: HttpClient) {}

  login(username: string, password: string) {
    return this.http.post<User>(`${this.apiUrl}/login`, {
      username,
      password
    }).pipe(
      tap(user => this.user.set(user))
    );
  }

  register(username: string, password: string, role: string) {
    return this.http.post<User>(`${this.apiUrl}/register`, {
      username,
      password,
      role
    });
  }

  logout() {
    this.user.set(null);
  }

  isLoggedIn() {
    return this.user() !== null;
  }
}
