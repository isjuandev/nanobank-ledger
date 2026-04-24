import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <main class="flex min-h-screen items-center justify-center bg-slate-50 p-4">
      <section class="grid w-full max-w-4xl overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm md:grid-cols-[0.95fr_1.05fr]">
        <div class="hidden bg-slate-950 p-8 text-white md:flex md:flex-col md:justify-between">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wide text-emerald-300">NanoBank Ledger</p>
            <h1 class="mt-3 text-3xl font-semibold leading-tight">Tu dinero, organizado desde el día uno.</h1>
          </div>
          <div class="space-y-4">
            <div class="rounded-lg border border-white/10 bg-white/10 p-4">
              <p class="text-sm text-slate-300">Empieza con</p>
              <p class="mt-2 text-3xl font-semibold">0 comisiones</p>
            </div>
            <div class="grid grid-cols-2 gap-3 text-sm">
              <div class="rounded-lg border border-white/10 bg-white/10 p-3">
                <p class="text-slate-300">Ahorro</p>
                <p class="mt-1 text-xl font-semibold">24/7</p>
              </div>
              <div class="rounded-lg border border-white/10 bg-white/10 p-3">
                <p class="text-slate-300">Control</p>
                <p class="mt-1 text-xl font-semibold">Total</p>
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 sm:p-8">
          <div class="mb-6">
            <p class="text-xs font-semibold uppercase tracking-wide text-emerald-700 md:hidden">NanoBank Ledger</p>
            <h2 class="mt-1 text-2xl font-semibold text-slate-950">Crear cuenta</h2>
            <p class="mt-1 text-sm text-slate-500">Regístrate para entrar a tu panel financiero.</p>
          </div>

          @if (error()) {
            <p class="mb-3 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{{ error() }}</p>
          }

          <form class="space-y-4" [formGroup]="form" (ngSubmit)="submit()">
            <div>
              <label for="name" class="mb-1 block text-sm font-medium text-slate-700">Nombre</label>
              <input
                id="name"
                type="text"
                formControlName="name"
                class="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm"
                autocomplete="name"
              />
            </div>

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
                autocomplete="new-password"
              />
            </div>

            <button
              type="submit"
              class="w-full rounded-md bg-slate-950 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              [disabled]="loading() || form.invalid"
            >
              {{ loading() ? 'Creando...' : 'Crear cuenta' }}
            </button>
          </form>

          <p class="mt-4 text-center text-sm text-slate-600">
            ¿Ya tienes cuenta?
            <a href="/login" class="font-medium text-emerald-700 hover:text-emerald-600">Iniciar sesión</a>
          </p>
        </div>
      </section>
    </main>
  `
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  submit(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.register(this.form.getRawValue()).subscribe({
      next: () => {
        this.loading.set(false);
        void this.router.navigateByUrl('/dashboard');
      },
      error: () => {
        this.loading.set(false);
        this.error.set('No fue posible registrar la cuenta');
      }
    });
  }
}
