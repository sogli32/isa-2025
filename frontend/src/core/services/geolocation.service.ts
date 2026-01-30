import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

/**
 * Interfejs za lokaciju korisnika
 */
export interface UserLocation {
  latitude: number | null;
  longitude: number | null;
  city: string | null;
  country: string | null;
  source: 'browser' | 'ip_geolocation' | 'unknown';
  approximate: boolean;
}

/**
 * Request za slanje lokacije na backend
 */
export interface UserLocationRequest {
  latitude: number | null;
  longitude: number | null;
  locationGranted: boolean;
}

/**
 * Servis za geolokaciju korisnika.
 * 
 * [S2] Zatražiti lokaciju od korisnika, u slučaju da odbije, 
 * koristiti aproksimaciju lokacije sa koje je upućen zahtev.
 */
@Injectable({
  providedIn: 'root'
})
export class GeolocationService {

  private apiUrl = 'http://localhost:8080/api/geolocation';
  
  // Keširana lokacija
  private cachedLocation: UserLocation | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Dobija lokaciju korisnika.
   * 1. Prvo pokušava browser Geolocation API
   * 2. Ako korisnik odbije ili nije dostupno -> backend koristi IP geolokaciju
   */
  getUserLocation(): Observable<UserLocation> {
    // Ako imamo keširanu lokaciju, vrati je
    if (this.cachedLocation) {
      return of(this.cachedLocation);
    }

    return new Observable<UserLocation>(observer => {
      // Proveri da li browser podržava geolokaciju
      if (!('geolocation' in navigator)) {
        console.log('Geolocation nije podržan u ovom browseru');
        this.getLocationFromBackend(false, null, null).subscribe({
          next: (location) => {
            this.cachedLocation = location;
            observer.next(location);
            observer.complete();
          },
          error: (err) => observer.error(err)
        });
        return;
      }

      // Zatraži lokaciju od korisnika
      navigator.geolocation.getCurrentPosition(
        // SUCCESS - korisnik dozvolio
        (position) => {
          console.log('Korisnik dozvolio geolokaciju');
          const lat = position.coords.latitude;
          const lng = position.coords.longitude;
          
          this.getLocationFromBackend(true, lat, lng).subscribe({
            next: (location) => {
              this.cachedLocation = location;
              observer.next(location);
              observer.complete();
            },
            error: (err) => observer.error(err)
          });
        },
        // ERROR - korisnik odbio ili greška
        (error) => {
          console.log('Korisnik odbio geolokaciju ili greška:', error.message);
          this.getLocationFromBackend(false, null, null).subscribe({
            next: (location) => {
              this.cachedLocation = location;
              observer.next(location);
              observer.complete();
            },
            error: (err) => observer.error(err)
          });
        },
        // Opcije
        {
          enableHighAccuracy: false,
          timeout: 10000,
          maximumAge: 300000 // 5 minuta cache
        }
      );
    });
  }

  /**
   * Šalje lokaciju na backend za resolving.
   */
  private getLocationFromBackend(
    granted: boolean, 
    lat: number | null, 
    lng: number | null
  ): Observable<UserLocation> {
    const request: UserLocationRequest = {
      latitude: lat,
      longitude: lng,
      locationGranted: granted
    };

    return this.http.post<UserLocation>(`${this.apiUrl}/resolve`, request).pipe(
      catchError(err => {
        console.error('Greška pri dobijanju lokacije sa backend-a:', err);
        // Fallback na unknown lokaciju
        return of({
          latitude: null,
          longitude: null,
          city: null,
          country: null,
          source: 'unknown' as const,
          approximate: true
        });
      })
    );
  }

  /**
   * Dobija lokaciju samo na osnovu IP adrese (bez pitanja korisnika).
   */
  getIpLocation(): Observable<UserLocation> {
    return this.http.get<UserLocation>(`${this.apiUrl}/ip-location`).pipe(
      catchError(err => {
        console.error('Greška pri IP geolokaciji:', err);
        return of({
          latitude: null,
          longitude: null,
          city: null,
          country: null,
          source: 'unknown' as const,
          approximate: true
        });
      })
    );
  }

  /**
   * Briše keširanu lokaciju (npr. ako korisnik želi da promeni).
   */
  clearCache(): void {
    this.cachedLocation = null;
  }

  /**
   * Vraća keširanu lokaciju ako postoji.
   */
  getCachedLocation(): UserLocation | null {
    return this.cachedLocation;
  }
}
