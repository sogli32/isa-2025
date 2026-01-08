import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AppService } from '../../app.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.html',
  styleUrls: ['./register.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
})
export class RegisterComponent {
  username: string = '';
  email: string = '';
  password: string = '';
  confirmPassword: string = '';
  firstName: string = '';
  lastName: string = '';
  address: string = '';
  error: string = '';
  success: string = '';

  constructor(private appService: AppService, private cdr: ChangeDetectorRef, private router: Router) {}

  register() {
    this.error = '';
    this.success = '';

    // Validacija svih polja
    if (!this.username || !this.email || !this.password || !this.confirmPassword || !this.firstName || !this.lastName || !this.address) {
      this.error = 'Molimo unesite sve podatke';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.error = 'Lozinke se ne poklapaju';
      return;
    }

    // Poziv servisa za registraciju
    this.appService.register({
      username: this.username,
      email: this.email,
      password: this.password,
      confirmPassword: this.confirmPassword,
      firstName: this.firstName,
      lastName: this.lastName,
      address: this.address,
      role: 'USER'
    }).subscribe({
      next: (response) => {
        this.success = 'Uspešno ste registrovani! Možete se prijaviti.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err.error?.error || 'Greška prilikom registracije';
        this.cdr.detectChanges();
      }
    });
  }

  navigateToLogin() {
    this.router.navigate(['/auth/login']);
  }
}
