import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AppService } from '../../app.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule]
})
export class LoginComponent {
  username: string = '';
  password: string = '';
  error: string = '';
  success: string = '';

  constructor(private appService: AppService, private cdr: ChangeDetectorRef) {}
login() {
  this.error = '';
  this.success = '';

  if (!this.username || !this.password) {
    this.error = 'Please enter username and password';
    return;
  }

  this.appService.login({
    username: this.username,
    password: this.password
  }).subscribe({
    next: (response) => {
      this.success = `Welcome ${response.username} (${response.role})`;
      this.cdr.detectChanges();
    },
    error: (err) => {
      this.error = 'Invalid credentials';
      this.cdr.detectChanges();
    }
  });
}

}