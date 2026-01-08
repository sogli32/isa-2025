import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule, NgClass } from '@angular/common';

@Component({
  selector: 'app-activate-account',
  standalone: true,
  imports: [CommonModule, NgClass],
  templateUrl: './activate-account.html',
  styleUrls: ['./activate-account.css']
})
export class ActivateAccountComponent implements OnInit {
  token: string = '';
  message: string = 'Activating your account...';
  isError: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

 ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';

    if (!this.token) {
        this.isError = true;
        this.message = 'No activation token found.';
        this.cdr.detectChanges(); 
        return;
    }

    this.authService.activateAccount(this.token).subscribe({
        next: (response) => {
            console.log('Activation successful:', response);
            this.message = 'Your account has been activated successfully!';
            this.isError = false;
            this.cdr.detectChanges(); 
            
            setTimeout(() => this.router.navigate(['/auth/login']), 2000);
        },
        error: (err) => {
            console.error('Activation error:', err);
            
            if (err.status === 400 && err.error === 'Invalid activation token') {
                this.message = 'Account already activated. Redirecting to login...';
                this.isError = false;
            } else {
                this.isError = true;
                this.message = 'Activation failed or token expired.';
            }
            this.cdr.detectChanges();
            setTimeout(() => this.router.navigate(['/auth/login']), 2000);
        }
    });
}
}