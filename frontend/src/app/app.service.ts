import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AppService {
  constructor(private http: HttpClient) {}

  getMessage() {
    return this.http.get<{ message: string }>('http://localhost:8080/api/message')
      .pipe(
        catchError(err => {
          console.error('Error fetching message:', err);
          return of({ message: 'Error connecting to Spring Boot' });
        })
      );
  }
}
