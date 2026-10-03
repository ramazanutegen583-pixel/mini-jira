import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, tap, throwError } from 'rxjs';
import { Session } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly session = signal<Session | null>(null);
  private expiryTimer?: ReturnType<typeof setTimeout>;
  readonly user = computed(() => this.session()?.user ?? null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  token(): string | null {
    const session = this.session();
    return session && Date.parse(session.expiresAt) > Date.now() ? session.accessToken : null;
  }
  login(email: string, password: string) {
    return this.http.post<Session>('/api/auth/login', { email, password }).pipe(tap(session => {
      this.session.set(session);
      clearTimeout(this.expiryTimer);
      this.expiryTimer = setTimeout(() => this.logout(), Math.max(0, Date.parse(session.expiresAt) - Date.now()));
    }));
  }
  logout() { clearTimeout(this.expiryTimer); this.session.set(null); void this.router.navigateByUrl('/auth/login'); }
}
export const authGuard: CanActivateFn = () => inject(AuthService).token() ? true : inject(Router).createUrlTree(['/auth/login']);
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const apiRequest = request.url.startsWith('/api/');
  const authorized = apiRequest && token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;
  return next(authorized).pipe(catchError((error: HttpErrorResponse) => {
    if (apiRequest && error.status === 401 && !request.url.endsWith('/login')) auth.logout();
    return throwError(() => error);
  }));
};
