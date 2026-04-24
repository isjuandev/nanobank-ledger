import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <main class="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <section class="grid w-full max-w-4xl overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm md:grid-cols-[0.95fr_1.05fr]">
        <div class="hidden bg-slate-950 p-8 text-white md:flex md:flex-col md:justify-between">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wide text-emerald-300">NanoBank Ledger</p>
            <h1 class="mt-3 text-3xl font-semibold leading-tight">Control claro para tus billeteras.</h1>
          </div>
          <div class="space-y-4">
            <div class="rounded-lg border border-white/10 bg-white/10 p-4">
              <p class="text-sm text-slate-300">Balance total</p>
              <p class="mt-2 text-3xl font-semibold">$24,850.00</p>
            </div>
            <div class="grid grid-cols-2 gap-3 text-sm">
              <div class="rounded-lg border border-white/10 bg-white/10 p-3">
                <p class="text-slate-300">Billeteras</p>
                <p class="mt-1 text-xl font-semibold">6</p>
              </div>
              <div class="rounded-lg border border-white/10 bg-white/10 p-3">
                <p class="text-slate-300">Movimientos</p>
                <p class="mt-1 text-xl font-semibold">128</p>
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 sm:p-8">
          <div class="mb-6">
            <p class="text-xs font-semibold uppercase tracking-wide text-emerald-700 md:hidden">NanoBank Ledger</p>
            <h2 class="mt-1 text-2xl font-semibold text-slate-950">Iniciar sesión</h2>
            <p class="mt-1 text-sm text-slate-500">Ingresa con tu cuenta para continuar al panel.</p>
          </div>

          @if (error()) {
            <p class="mb-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{{ error() }}</p>
          }

          <form class="space-y-4" [formGroup]="form" (ngSubmit)="submit()">
            <div>
              <label for="email" class="mb-1 block text-sm font-medium text-slate-700">Email</label>
              <input
                id="email"
                type="email"
                formControlName="email"
                class="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm"
                autocomplete="email"
              />
            </div>

            <div>
              <label for="password" class="mb-1 block text-sm font-medium text-slate-700">Contraseña</label>
              <input
                id="password"
                type="password"
                formControlName="password"
                class="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm"
                autocomplete="current-password"
              />
            </div>

            <button
              type="submit"
              class="w-full rounded-md bg-slate-950 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              [disabled]="loading() || form.invalid"
            >
              {{ loading() ? 'Ingresando...' : 'Ingresar' }}
            </button>
          </form>
          <p class="mt-4 text-center text-sm text-slate-600">
            ¿No tienes cuenta?
            <a href="/register" class="font-medium text-emerald-700 hover:text-emerald-600">Crear cuenta</a>
          </p>
        </div>
      </section>
    </main>
  `
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigateByUrl('/dashboard');
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Credenciales inválidas');
      }
    });
  }
}
