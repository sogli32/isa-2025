import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AppService } from '../../app.service';
import { AuthService } from '../../../core/services/auth.service';
import { GeolocationService } from '../../../core/services/geolocation.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
})
export class LoginComponent {
  email: string = '';
  password: string = '';
  error: string = '';
  success: string = '';

  constructor(
    private authService: AuthService,
    private geolocationService: GeolocationService,
    private cdr: ChangeDetectorRef,
    private router: Router
  ) {}

  login() {
    this.error = '';
    this.success = '';

    if (!this.email || !this.password) {
      this.error = 'Molimo unesite email i lozinku';
      return;
    }

    this.authService.login(this.email, this.password).subscribe({
      next: (user) => {
        this.success = `Dobrodošli ${user.username} (${user.role})`;
        this.cdr.detectChanges();

        // [S2] Zatraži lokaciju od korisnika nakon uspešnog logina
        this.requestUserLocation();

        // Redirect na home ili dashboard
        this.router.navigate(['/']);
      },
      error: (err) => {
        if (err.status === 429) {
          this.error = 'Previše pokušaja prijave. Pokušajte ponovo za minut.';
        } else if (err.status === 401) {
          this.error = 'Pogrešan email ili lozinka.';
        } else {
          this.error = 'Došlo je do greške. Pokušajte kasnije.';
        }
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * [S2] Zatražiti lokaciju od korisnika, u slučaju da odbije, 
   * koristiti aproksimaciju lokacije sa koje je upućen zahtev.
   */
  private requestUserLocation(): void {
    this.geolocationService.getUserLocation().subscribe({
      next: (location) => {
        console.log('Lokacija korisnika:', location);
        console.log('Izvor:', location.source);
        console.log('Približna:', location.approximate);
        
        // Lokacija je sačuvana u servisu (keširana)
        // Može se koristiti kasnije za trending videe u blizini
      },
      error: (err) => {
        console.error('Greška pri dobijanju lokacije:', err);
        // Ne prikazujemo grešku korisniku - lokacija nije kritična
      }
    });
  }

  navigateToRegister() {
    this.router.navigate(['/auth/register']);
  }
}