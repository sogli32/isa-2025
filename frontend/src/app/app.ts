import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterModule, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../core/services/auth.service';
import { of } from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrls: ['./app.css'],
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterModule]
})
export class App implements OnInit {
  message: string = 'Loading...';

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    public authService: AuthService, // public da se koristi u HTML
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadMessage();
  }

  private loadMessage(): void {
    this.http.get('http://localhost:8080/hello', { responseType: 'text' })
      .pipe(
        catchError(err => {
          console.error('Error connecting to backend:', err);
          return of('Error connecting to backend');
        })
      )
      .subscribe((data: string) => {
        console.log('Received data:', data);
        this.message = data;
        this.cdr.detectChanges();
      });
  }

  logout(): void {
    this.authService.logout(); // metoda u AuthService koja briše token / korisnika
    this.router.navigate(['/auth/login']);
  }
}
