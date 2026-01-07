import { Routes } from '@angular/router';
import { authRoutes } from './auth/auth.routes';

export const routes: Routes = [
  { path: '', redirectTo: '/auth/login', pathMatch: 'full' },
  { path: 'auth', children: authRoutes }
];
