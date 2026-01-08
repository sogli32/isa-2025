import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AppService } from '../../app.service';

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

  constructor(private appService: AppService, private cdr: ChangeDetectorRef, private router: Router) {}

  login() {
    this.error = '';
    this.success = '';

    if (!this.email || !this.password) {
      this.error = 'Molimo unesite email i lozinku';
      return;
    }

    this.appService.login({
      email: this.email,
      password: this.password
    }).subscribe({
      next: (response) => {
        this.success = `Dobrodošli ${response.username} (${response.role})`;
        this.cdr.detectChanges();
        // Opcionalno: redirect na home ili dashboard
        // this.router.navigate(['/']);
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

  navigateToRegister() {
    this.router.navigate(['/auth/register']);
  }
}
