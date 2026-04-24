import { CurrencyPipe, DatePipe, NgClass } from '@angular/common';
import { Component, input } from '@angular/core';
import { CdkDrag } from '@angular/cdk/drag-drop';

import { Transaction, TransactionType } from '../../core/models/transaction.model';

@Component({
  selector: 'app-transaction-item',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, NgClass],
  hostDirectives: [CdkDrag],
  host: {
    class: 'block cursor-move'
  },
  template: `
    <article class="rounded-md border border-slate-200 bg-white px-3 py-2 shadow-xs">
      <div class="flex items-center justify-between gap-2">
        <div>
          <p class="text-sm font-medium text-slate-900">{{ transaction().category }}</p>
          <p class="text-xs text-slate-500">{{ transaction().date | date:'medium' }}</p>
        </div>
        <div class="text-right">
          <p
            class="text-sm font-semibold"
            [ngClass]="{
              'text-emerald-600': transaction().type === transactionType.INGRESO,
              'text-red-600': transaction().type === transactionType.GASTO
            }"
          >
            {{ transaction().amount | currency:'USD':'symbol':'1.2-2' }}
          </p>
          <p class="text-[11px] uppercase tracking-wide text-slate-500">{{ transactionTypeLabel(transaction().type) }}</p>
        </div>
      </div>
    </article>
  `
})
export class TransactionItemComponent {
  readonly transaction = input.required<Transaction>();
  protected readonly transactionType = TransactionType;

  transactionTypeLabel(type: TransactionType): string {
    switch (type) {
      case TransactionType.INGRESO:
        return 'Ingreso';
      case TransactionType.GASTO:
        return 'Gasto';
    }
  }
}
