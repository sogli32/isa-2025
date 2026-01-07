import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <h2>Welcome to ISA Project</h2>
    <p>Please login to continue.</p>
    <a routerLink="/auth/login">Login</a>
  `
})
export class HomeComponent {}
