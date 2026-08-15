import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { LoginRequest, LoginResponse, UsuarioAutenticado } from '../../models/auth.model';
import { AuthStore } from './auth.store';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authStore = inject(AuthStore);
  private readonly tokenService = inject(TokenService);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  login(payload: LoginRequest): Observable<UsuarioAutenticado> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, payload).pipe(
      map((response) => {
        this.tokenService.saveTokens(response.accessToken, response.refreshToken);
        localStorage.setItem('sri_files_usuario', JSON.stringify(response.usuario));
        this.authStore.setUsuario(response.usuario);
        return response.usuario;
      })
    );
  }

  restoreSession(): Observable<UsuarioAutenticado | null> {
    const token = this.tokenService.getToken();
    if (!token) {
      return of(null);
    }

    return this.http.get<UsuarioAutenticado>(`${this.apiUrl}/me`).pipe(
      tap((usuario) => {
        localStorage.setItem('sri_files_usuario', JSON.stringify(usuario));
        this.authStore.setUsuario(usuario);
      }),
      catchError(() => {
        this.tokenService.clear();
        localStorage.removeItem('sri_files_usuario');
        this.authStore.setUsuario(null);
        return of(null);
      })
    );
  }

  logout(): void {
    this.tokenService.clear();
    localStorage.removeItem('sri_files_usuario');
    this.authStore.setUsuario(null);
    void this.router.navigate(['/login']);
  }
}
