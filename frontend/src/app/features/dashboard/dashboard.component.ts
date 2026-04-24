import { CommonModule, CurrencyPipe } from '@angular/common';
import { CdkDropListGroup } from '@angular/cdk/drag-drop';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Transaction, TransactionType } from '../../core/models/transaction.model';
import { Wallet, WalletType } from '../../core/models/wallet.model';
import { AuthService } from '../../core/services/auth.service';
import { TransactionMovementEvent, TransactionService } from '../../core/services/transaction.service';
import { WalletService } from '../../core/services/wallet.service';
import { TransactionListComponent } from '../transactions/transaction-list.component';
import { WalletCardComponent } from '../wallets/wallet-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, FormsModule, CdkDropListGroup, WalletCardComponent, TransactionListComponent],
  template: `
    <main class="min-h-screen bg-slate-50">
      <section class="border-b border-slate-200 bg-white">
        <div class="mx-auto flex w-full max-w-[1440px] flex-col gap-4 px-4 py-5 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8">
          <div>
            <p class="text-xs font-semibold uppercase tracking-wide text-emerald-700">NanoBank Ledger</p>
            <h1 class="mt-1 text-2xl font-semibold text-slate-950">Panel financiero</h1>
            <p class="mt-1 text-sm text-slate-500">Administra billeteras, balances y movimientos desde una vista central.</p>
          </div>

          <div class="flex flex-col gap-3 sm:flex-row sm:items-center lg:justify-end">
            <div class="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 sm:min-w-34">
              <p class="text-xs text-slate-500">Balance total</p>
              <p class="text-2xl font-semibold text-slate-950">
                {{ totalBalance() | currency:'USD':'symbol':'1.2-2' }}
              </p>
            </div>
            <div class="flex min-w-0 items-center gap-3 rounded-lg border border-slate-200 bg-white px-3 py-2 sm:min-w-52">
              <div class="flex h-10 w-10 items-center justify-center rounded-full bg-emerald-100 text-sm font-semibold text-emerald-700">
                {{ userInitial() }}
              </div>
              <div class="min-w-0">
                <p class="truncate text-sm font-medium text-slate-900">{{ currentUser()?.name || 'Usuario' }}</p>
                <p class="truncate text-xs text-slate-500">{{ currentUser()?.email || 'Sin correo' }}</p>
              </div>
            </div>
            <button
              type="button"
              class="inline-flex items-center justify-center rounded-md border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:border-red-200 hover:bg-red-50 hover:text-red-700 focus:outline-none focus:ring-2 focus:ring-red-200"
              (click)="logout()"
            >
              Cerrar sesión
            </button>
          </div>
        </div>
      </section>

      <div class="mx-auto grid w-full max-w-[1440px] grid-cols-1 gap-6 px-4 py-6 sm:px-6 lg:px-8 xl:grid-cols-[minmax(0,1fr)_430px]">
        <div class="space-y-6">
          <section class="grid grid-cols-1 gap-4 md:grid-cols-3">
            <div class="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
              <p class="text-sm text-slate-500">Billeteras</p>
              <p class="mt-2 text-3xl font-semibold text-slate-950">{{ wallets().length }}</p>
            </div>
            <div class="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
              <p class="text-sm text-slate-500">Balance promedio</p>
              <p class="mt-2 text-3xl font-semibold text-slate-950">
                {{ averageBalance() | currency:'USD':'symbol':'1.2-2' }}
              </p>
            </div>
            <div class="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
              <p class="text-sm text-slate-500">Transacciones registradas</p>
              <p class="mt-2 text-3xl font-semibold text-slate-950">{{ transactionCount() }}</p>
            </div>
          </section>

          @if (loading()) {
            <p class="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-600 shadow-sm">Cargando billeteras...</p>
          } @else {
            @if (error()) {
              <p class="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{{ error() }}</p>
            }
            <section cdkDropListGroup class="grid grid-cols-1 gap-5 2xl:grid-cols-2">
              @for (wallet of wallets(); track wallet.id) {
                <div class="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
                  <app-wallet-card [wallet]="wallet" (walletDeleted)="onWalletDeleted($event)"></app-wallet-card>
                  <app-transaction-list [walletId]="wallet.id" [walletIds]="walletIds()"></app-transaction-list>
                </div>
              } @empty {
                <div class="rounded-lg border border-dashed border-slate-300 bg-white p-8 text-center">
                  <p class="text-sm font-medium text-slate-800">No hay billeteras registradas.</p>
                  <p class="mt-1 text-sm text-slate-500">Crea la primera para empezar a registrar movimientos.</p>
                </div>
              }
            </section>
          }
        </div>

        <aside class="space-y-6 xl:sticky xl:top-6 xl:self-start">
          <section class="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <div class="flex items-center justify-between gap-2">
              <h2 class="text-base font-semibold text-slate-950">Crear billetera</h2>
            </div>
            <p class="mt-1 text-sm text-slate-500">Se agregará al inicio de tu panel.</p>
            <form class="mt-4 space-y-3" (ngSubmit)="createWallet()">
              <input
                type="text"
                name="name"
                class="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm"
                placeholder="Nombre (ej. Ahorros)"
                [ngModel]="newWalletName()"
                (ngModelChange)="newWalletName.set($event)"
              />
              <select
                name="type"
                class="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm"
                [ngModel]="newWalletType()"
                (ngModelChange)="onWalletTypeChange($event)"
              >
                @for (type of walletTypeOptions; track type) {
                  <option [value]="type">{{ walletTypeLabel(type) }}</option>
                }
              </select>
              <button
                type="submit"
                class="w-full rounded-md bg-slate-950 px-4 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
                [disabled]="creatingWallet() || !newWalletName().trim()"
              >
                {{ creatingWallet() ? 'Creando...' : 'Crear billetera' }}
              </button>
            </form>
            @if (createError()) {
              <p class="mt-2 text-sm text-red-600">{{ createError() }}</p>
            }
          </section>

          <section class="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
            <div class="flex items-center justify-between gap-2">
              <h2 class="text-base font-semibold text-slate-950">Movimientos recientes</h2>
              <span class="text-xs text-slate-500">Últimos 5</span>
            </div>
            @if (recentMovementsLoading()) {
              <p class="mt-3 text-sm text-slate-500">Cargando movimientos...</p>
            } @else if (!recentMovements().length) {
              <p class="mt-3 text-sm text-slate-500">Aún no hay movimientos recientes.</p>
            } @else {
              <div class="mt-3 overflow-x-auto">
                <table class="min-w-full border-separate border-spacing-0 text-sm">
                  <thead>
                    <tr class="text-left text-xs uppercase tracking-wide text-slate-500">
                      <th class="border-b border-slate-200 px-2 py-2 font-medium">Fecha</th>
                      <th class="border-b border-slate-200 px-2 py-2 font-medium">Descripción</th>
                      <th class="border-b border-slate-200 px-2 py-2 font-medium">Categoría</th>
                      <th class="border-b border-slate-200 px-2 py-2 font-medium">Billetera</th>
                      <th class="border-b border-slate-200 px-2 py-2 text-right font-medium">Monto</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (movement of recentMovements(); track movement.id) {
                      <tr class="text-slate-700">
                        <td class="border-b border-slate-100 px-2 py-2 whitespace-nowrap">
                          {{ movement.date | date:'yyyy-MM-dd HH:mm' }}
                        </td>
                        <td class="border-b border-slate-100 px-2 py-2">
                          {{ movement.description || 'Sin descripción' }}
                        </td>
                        <td class="border-b border-slate-100 px-2 py-2">{{ movement.category }}</td>
                        <td class="border-b border-slate-100 px-2 py-2">{{ movement.walletName }}</td>
                        <td
                          class="border-b border-slate-100 px-2 py-2 text-right font-semibold whitespace-nowrap"
                          [class.text-emerald-700]="movement.type === transactionType.INGRESO"
                          [class.text-red-600]="movement.type === transactionType.GASTO"
                        >
                          {{ movement.type === transactionType.INGRESO ? '+' : '-' }}
                          {{ movement.amount | currency:'USD':'symbol':'1.2-2' }}
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </section>
        </aside>
      </div>
    </main>
  `
})
export class DashboardComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly transactionService = inject(TransactionService);
  private readonly walletService = inject(WalletService);

  readonly wallets = signal<Wallet[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly creatingWallet = signal(false);
  readonly createError = signal<string | null>(null);
  readonly recentMovements = signal<Transaction[]>([]);
  readonly recentMovementsLoading = signal(false);
  readonly newWalletName = signal('');
  readonly newWalletType = signal<WalletType>(WalletType.AHORROS);
  readonly walletTypeOptions = Object.values(WalletType);
  readonly transactionType = TransactionType;

  readonly totalBalance = computed(() =>
    this.wallets().reduce((acc, wallet) => acc + wallet.balance, 0)
  );
  readonly averageBalance = computed(() => {
    const wallets = this.wallets();
    return wallets.length ? this.totalBalance() / wallets.length : 0;
  });
  readonly transactionCount = computed(() =>
    this.wallets().reduce((acc, wallet) => acc + wallet.transactionCount, 0)
  );
  readonly currentUser = this.authService.currentUser;
  readonly walletIds = computed(() => this.wallets().map((wallet) => wallet.id));
  readonly userInitial = computed(() => {
    const name = this.currentUser()?.name?.trim();
    if (!name) {
      return '?';
    }
    return name.charAt(0).toUpperCase();
  });

  ngOnInit(): void {
    this.transactionService.movements$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => {
        this.applyRecentMovementEvent(event);
        this.loadWallets(false, false);
      });
    this.loadWallets();
  }

  onWalletDeleted(walletId: number): void {
    this.error.set(null);
    const previous = [...this.wallets()];
    this.wallets.update((wallets) => wallets.filter((wallet) => wallet.id !== walletId));

    this.walletService.deleteWallet(walletId).subscribe({
      next: () => {
        this.error.set(null);
      },
      error: (err: HttpErrorResponse) => {
        this.wallets.set(previous);
        const backendMessage = typeof err.error?.message === 'string' ? err.error.message : null;
        this.error.set(backendMessage ?? 'No se pudo eliminar la billetera.');
      }
    });
  }

  createWallet(): void {
    const name = this.newWalletName().trim();
    const type = this.normalizeWalletType(this.newWalletType());

    if (!name || !type || this.creatingWallet()) {
      this.createError.set('Selecciona un tipo de billetera válido.');
      return;
    }

    this.creatingWallet.set(true);
    this.createError.set(null);

    this.walletService.createWallet({ name, type }).subscribe({
      next: (wallet) => {
        this.wallets.update((current) => [wallet, ...current]);
        this.newWalletName.set('');
        this.newWalletType.set(WalletType.AHORROS);
        this.creatingWallet.set(false);
      },
      error: () => {
        this.createError.set('No se pudo crear la billetera.');
        this.creatingWallet.set(false);
      }
    });
  }

  logout(): void {
    this.authService.logout();
    void this.router.navigateByUrl('/login');
  }

  walletTypeLabel(type: WalletType): string {
    switch (type) {
      case WalletType.AHORROS:
        return 'Ahorros';
      case WalletType.GASTOS:
        return 'Gastos';
      case WalletType.INVERSION:
        return 'Inversión';
    }
  }

  onWalletTypeChange(type: unknown): void {
    const normalized = this.normalizeWalletType(type);
    if (normalized) {
      this.newWalletType.set(normalized);
    }
  }

  private normalizeWalletType(type: unknown): WalletType | null {
    if (type === WalletType.AHORROS || type === WalletType.GASTOS || type === WalletType.INVERSION) {
      return type;
    }
    return null;
  }

  private loadWallets(showLoading = true, refreshRecentMovements = true): void {
    if (showLoading) {
      this.loading.set(true);
    }
    this.error.set(null);

    this.walletService.getWallets().subscribe({
      next: (wallets) => {
        this.wallets.set(wallets);
        if (refreshRecentMovements) {
          this.loadRecentMovements(wallets);
        }
        if (showLoading) {
          this.loading.set(false);
        }
      },
      error: () => {
        this.error.set('No se pudieron cargar las billeteras.');
        if (showLoading) {
          this.loading.set(false);
        }
      }
    });
  }

  private loadRecentMovements(wallets: Wallet[]): void {
    if (!wallets.length) {
      this.recentMovements.set([]);
      this.recentMovementsLoading.set(false);
      return;
    }

    this.recentMovementsLoading.set(true);

    forkJoin(wallets.map((wallet) => this.transactionService.getTransactions(wallet.id))).subscribe({
      next: (walletTransactions) => {
        const merged = walletTransactions
          .flat()
          .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
          .slice(0, 5);
        this.recentMovements.set(merged);
        this.recentMovementsLoading.set(false);
      },
      error: () => {
        this.recentMovements.set([]);
        this.recentMovementsLoading.set(false);
      }
    });
  }

  private applyRecentMovementEvent(event: TransactionMovementEvent): void {
    if (event.type === 'deleted') {
      this.recentMovements.update((current) =>
        current.filter((movement) => movement.id !== event.transactionId)
      );
      return;
    }

    this.recentMovements.update((current) => {
      const updated = [event.transaction, ...current.filter((movement) => movement.id !== event.transaction.id)];
      return updated
        .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
        .slice(0, 5);
    });
  }
}
