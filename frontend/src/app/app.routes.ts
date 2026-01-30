// app.routes.ts
import { Routes } from '@angular/router';
import { LoginComponent } from './auth/login/login';
import { RegisterComponent } from './auth/register/register';
import { ActivateAccountComponent } from './auth/activate-account/activate-account';
import { VideoListComponent } from './videos/video-list/video-list';
import { VideoPlayerComponent } from './videos/video-player/video-player';
import { UploadVideoComponent } from './videos/upload-video/upload-video';
import { BenchmarkComponent } from './benchmark/benchmark';

export const routes: Routes = [
  { path: '', component: VideoListComponent }, // HOME
  { 
    path: 'auth', 
    children: [
      { path: 'login', component: LoginComponent },
      { path: 'register', component: RegisterComponent },
      { path: 'activate', component: ActivateAccountComponent }
    ]
  },
  { path: 'upload', component: UploadVideoComponent },
  { path: 'video/:id', component: VideoPlayerComponent },
  { path: 'benchmark', component: BenchmarkComponent }, // NOVO - Benchmark stranica
  { path: '**', redirectTo: '' }
];
