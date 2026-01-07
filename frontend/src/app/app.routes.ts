import { Routes } from '@angular/router';
import { authRoutes } from './auth/auth.routes';
import { HomeComponent } from './home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'auth', children: authRoutes }
];
