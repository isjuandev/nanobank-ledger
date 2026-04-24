import { CurrencyPipe, NgClass } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { Wallet, WalletType } from '../../core/models/wallet.model';

@Component({
  selector: 'app-wallet-card',
  standalone: true,
  imports: [CurrencyPipe, NgClass],
  template: `
    <article class="border-b border-slate-200 bg-white p-4">
      <header class="mb-4 flex items-start justify-between gap-4">
        <div class="min-w-0">
          <h3 class="text-base font-semibold text-slate-900">{{ wallet().name }}</h3>
          <p class="text-xs text-slate-500">{{ walletTypeLabel(wallet().type) }}</p>
        </div>
        <button
          type="button"
          class="rounded-md border border-red-200 px-2 py-1 text-xs font-medium text-red-600 hover:bg-red-50"
          (click)="onDelete()"
        >
          Eliminar
        </button>
      </header>

      <div class="space-y-1">
        <p class="text-xs text-slate-500">Balance</p>
        <p
          class="text-xl font-semibold"
          [ngClass]="wallet().balance >= 0 ? 'text-emerald-600' : 'text-red-600'"
        >
          {{ wallet().balance | currency:'USD':'symbol':'1.2-2' }}
        </p>
      </div>
    </article>
  `
})
export class WalletCardComponent {
  readonly wallet = input.required<Wallet>();
  readonly walletDeleted = output<number>();

  onDelete(): void {
    const accepted = window.confirm(`¿Eliminar la billetera "${this.wallet().name}"?`);
    if (!accepted) {
      return;
    }
    this.walletDeleted.emit(this.wallet().id);
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
}
