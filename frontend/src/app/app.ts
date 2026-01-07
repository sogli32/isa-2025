import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  styleUrls: ['./app.css'],
  standalone: true,
  imports: [CommonModule, RouterOutlet] 
})
export class App implements OnInit {
  message: string = 'Loading...';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

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
}
