import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { User } from '../models/user.model';
import { tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';

  // Inicijalizuj signal sa korisnikom iz localStorage
  user = signal<User | null>(this.getStoredUser());

  constructor(private http: HttpClient) {}

  // Metoda za dobijanje korisnika iz localStorage
  private getStoredUser(): User | null {
    try {
      const userStr = localStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    } catch (error) {
      console.error('Error parsing user from localStorage:', error);
      return null;
    }
  }
login(email: string, password: string) {
  return this.http.post<User>(`${this.apiUrl}/login`, {
    email,    // ovde je email, ne username
    password
  }).pipe(
    tap(user => {
      localStorage.setItem('user', JSON.stringify(user));
      this.user.set(user);
      console.log('User logged in:', user);
    })
  );
}


  register(username: string, password: string, role: string) {
    return this.http.post<User>(`${this.apiUrl}/register`, {
      username,
      password,
      role
    });
  }

  getToken(): string | null {
  const currentUser = this.user();
  if (currentUser && (currentUser as any).token) {
    return (currentUser as any).token; // cast na any ako User model nema token
  }

  // fallback: proveri localStorage
  const storedUserStr = localStorage.getItem('user');
  if (storedUserStr) {
    const storedUser = JSON.parse(storedUserStr);
    return storedUser.token ?? null;
  }

  return null;
}


  activateAccount(token: string) {
    const params = { token: token };
    return this.http.get(`${this.apiUrl}/activate`, { params });
  }

  logout() {
    // Obriši korisnika iz localStorage i signala
    localStorage.removeItem('user');
    this.user.set(null);
    console.log('User logged out'); // Debug
  }

  isLoggedIn() {
    return this.user() !== null;
  }
}