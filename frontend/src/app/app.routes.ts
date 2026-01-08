import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { LoginComponent } from './auth/login/login';
import { RegisterComponent } from './auth/register/register';
import { ActivateAccountComponent } from './auth/activate-account/activate-account';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { 
    path: 'auth', 
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent },
      { path: 'activate', component: ActivateAccountComponent }
    ]
  },
  { path: '**', redirectTo: '' }
];