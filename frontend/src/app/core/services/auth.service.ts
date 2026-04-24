import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { AuthResponse, LoginRequest, RegisterRequest, User } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/auth';
  private readonly tokenKey = 'nanobank_token';
  private readonly userKey = 'nanobank_user';

  private readonly _currentUser = signal<User | null>(this.readStoredUser());
  readonly currentUser = this._currentUser.asReadonly();

  readonly isAuthenticated = computed(() => {
    const token = localStorage.getItem(this.tokenKey);
    if (!token) {
      return false;
    }
    return this.isJwtTokenValid(token);
  });

  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, req).pipe(
      tap((response) => this.persistSession(response))
    );
  }

  register(req: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, req).pipe(
      tap((response) => this.persistSession(response))
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this._currentUser.set(null);
  }

  private persistSession(response: AuthResponse): void {
    const user: User = {
      id: response.userId,
      email: response.email,
      name: response.name
    };
    localStorage.setItem(this.tokenKey, response.token);
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this._currentUser.set(user);
  }

  private readStoredUser(): User | null {
    try {
      const stored = localStorage.getItem(this.userKey);
      if (!stored) {
        return null;
      }
      const parsed = JSON.parse(stored) as User;
      if (typeof parsed.id !== 'number' || typeof parsed.name !== 'string' || typeof parsed.email !== 'string') {
        return null;
      }
      return parsed;
    } catch {
      return null;
    }
  }

  private isJwtTokenValid(token: string): boolean {
    try {
      const payload = token.split('.')[1];
      if (!payload) {
        return false;
      }
      const parsed = JSON.parse(atob(payload)) as { exp?: number };
      if (!parsed.exp) {
        return true;
      }
      return parsed.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }
}
