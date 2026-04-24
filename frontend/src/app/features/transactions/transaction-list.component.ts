import { CommonModule } from '@angular/common';
import { CdkDragDrop, CdkDropList, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { Transaction, TransactionType } from '../../core/models/transaction.model';
import { TransactionMovementEvent, TransactionService } from '../../core/services/transaction.service';
import { TransactionItemComponent } from './transaction-item.component';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule, FormsModule, CdkDropList, TransactionItemComponent],
  template: `
    <section class="space-y-4 bg-slate-50 p-3 sm:p-4">
      <form
        class="grid grid-cols-2 gap-3 md:grid-cols-3"
        (ngSubmit)="createTransaction()"
      >
        <select
          class="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm"
          [ngModel]="newType()"
          name="type"
          (ngModelChange)="newType.set($event)"
        >
          <option [ngValue]="transactionType.INGRESO">Ingreso</option>
          <option [ngValue]="transactionType.GASTO">Gasto</option>
        </select>
        <input
          type="number"
          min="0.01"
          step="0.01"
          class="h-10 w-full rounded-md border border-slate-300 bg-white px-3 text-sm"
          placeholder="0.00"
          [ngModel]="newAmount()"
          name="amount"
          (ngModelChange)="newAmount.set($event)"
        />
        <input
          type="text"
          class="h-10 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm"
          placeholder="Cat."
          [ngModel]="newCategory()"
          name="category"
          (ngModelChange)="newCategory.set($event)"
        />
        <input
          type="text"
          class="h-10 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm md:col-span-1"
          placeholder="Desc."
          [ngModel]="newDescription()"
          name="description"
          (ngModelChange)="newDescription.set($event)"
        />
        <input
          type="date"
          class="h-10 w-full rounded-md border border-slate-300 bg-white px-2 text-sm"
          [ngModel]="newDate()"
          name="date"
          (ngModelChange)="newDate.set($event)"
        />
        <button
          type="submit"
          class="h-10 w-full rounded-md bg-slate-900 px-3 text-sm font-medium text-white disabled:opacity-60"
          [disabled]="creatingTransaction() || !canCreateTransaction()"
        >
          {{ creatingTransaction() ? 'Guardando...' : 'Agregar' }}
        </button>
      </form>
      @if (createError()) {
        <p class="text-xs text-red-600">{{ createError() }}</p>
      }

      <header class="space-y-2">
        <h4 class="text-sm font-semibold text-slate-800">Transacciones</h4>
        <div class="grid grid-cols-3 gap-3">
          <input
            type="text"
            class="h-9 rounded-md border border-slate-300 bg-white px-3 text-sm"
            placeholder="Cat."
            [ngModel]="categoryFilter()"
            (ngModelChange)="categoryFilter.set($event)"
          />
          <input
            type="date"
            class="h-9 rounded-md border border-slate-300 bg-white px-2 text-sm"
            [ngModel]="dateFromFilter()"
            (ngModelChange)="dateFromFilter.set($event)"
          />
          <input
            type="date"
            class="h-9 rounded-md border border-slate-300 bg-white px-2 text-sm"
            [ngModel]="dateToFilter()"
            (ngModelChange)="dateToFilter.set($event)"
          />
        </div>
      </header>

      <div
        class="min-h-24 space-y-2 rounded-md border border-dashed border-slate-300 bg-white/60 p-2"
        cdkDropList
        [id]="dropListId()"
        [cdkDropListData]="filteredTransactions()"
        [cdkDropListConnectedTo]="connectedDropListIds()"
        (cdkDropListDropped)="drop($event)"
      >
        @for (transaction of filteredTransactions(); track transaction.id) {
          <app-transaction-item [transaction]="transaction"></app-transaction-item>
        } @empty {
          <p class="text-xs text-slate-500">No hay transacciones</p>
        }
      </div>
    </section>
  `
})
export class TransactionListComponent implements OnInit {
  private readonly transactionService = inject(TransactionService);
  private readonly destroyRef = inject(DestroyRef);

  readonly walletId = input.required<number>();
  readonly walletIds = input.required<number[]>();

  readonly transactions = signal<Transaction[]>([]);
  readonly creatingTransaction = signal(false);
  readonly createError = signal<string | null>(null);

  readonly newType = signal<TransactionType>(TransactionType.GASTO);
  readonly newAmount = signal<string>('0.00');
  readonly newCategory = signal('');
  readonly newDescription = signal('');
  readonly newDate = signal('');

  readonly categoryFilter = signal('');
  readonly dateFromFilter = signal('');
  readonly dateToFilter = signal('');
  readonly transactionType = TransactionType;

  readonly filteredTransactions = computed(() => {
    const category = this.categoryFilter().trim().toLowerCase();
    const from = this.dateFromFilter();
    const to = this.dateToFilter();

    return this.transactions().filter((transaction) => {
      const matchesCategory = !category || transaction.category.toLowerCase().includes(category);
      const txDate = new Date(transaction.date);
      const matchesFrom = !from || txDate >= new Date(`${from}T00:00:00`);
      const matchesTo = !to || txDate <= new Date(`${to}T23:59:59`);
      return matchesCategory && matchesFrom && matchesTo;
    });
  });

  readonly dropListId = computed(() => `wallet-drop-${this.walletId()}`);
  readonly connectedDropListIds = computed(() =>
    this.walletIds()
      .filter((id) => id !== this.walletId())
      .map((id) => `wallet-drop-${id}`)
  );

  readonly canCreateTransaction = computed(() => {
    const amount = Number(this.newAmount());
    return amount > 0 && this.newCategory().trim().length > 0;
  });

  ngOnInit(): void {
    this.newDate.set(new Date().toISOString().slice(0, 10));
    this.transactionService.movements$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((event) => this.applyMovementEvent(event));
    this.loadTransactions();
  }

  createTransaction(): void {
    if (this.creatingTransaction() || !this.canCreateTransaction()) {
      return;
    }

    this.creatingTransaction.set(true);
    this.createError.set(null);

    this.transactionService.createTransaction({
      walletId: this.walletId(),
      type: this.newType(),
      amount: Number(this.newAmount()),
      category: this.newCategory().trim(),
      description: this.newDescription().trim() || undefined,
      date: this.newDate() ? this.buildBogotaDateTime(this.newDate()) : undefined
    }).subscribe({
      next: (created) => {
        this.newAmount.set('0.00');
        this.newCategory.set('');
        this.newDescription.set('');
        this.newType.set(TransactionType.GASTO);
        this.creatingTransaction.set(false);
      },
      error: () => {
        this.createError.set('No se pudo crear la transacción.');
        this.creatingTransaction.set(false);
      }
    });
  }

  drop(event: CdkDragDrop<Transaction[]>): void {
    if (event.previousContainer === event.container) {
      const current = [...this.filteredTransactions()];
      moveItemInArray(current, event.previousIndex, event.currentIndex);

      const reorderedIds = current.map((item) => item.id);
      const source = [...this.transactions()];
      const reordered = [
        ...current,
        ...source.filter((item) => !reorderedIds.includes(item.id))
      ];
      this.transactions.set(reordered);
      return;
    }

    const moved = event.previousContainer.data[event.previousIndex];
    if (!moved) {
      return;
    }

    const previousState = [...this.transactions()];
    const optimisticState = [...this.transactions(), { ...moved, walletId: this.walletId() }];
    this.transactions.set(optimisticState);

    this.transactionService.transferTransaction(moved.id, this.walletId(), moved.walletId).subscribe({
      next: (updated) => {
        this.transactions.update((current) => {
          const withoutTemp = current.filter((item) => item.id !== moved.id);
          return [...withoutTemp, updated];
        });
      },
      error: () => {
        this.transactions.set(previousState);
      }
    });
  }

  private applyMovementEvent(event: TransactionMovementEvent): void {
    if (event.type === 'created') {
      this.upsertTransaction(event.transaction);
      return;
    }

    if (event.type === 'deleted') {
      if (event.walletId === undefined || event.walletId === this.walletId()) {
        this.transactions.update((current) => current.filter((item) => item.id !== event.transactionId));
      }
      return;
    }

    this.applyTransferEvent(event.previousWalletId, event.transaction);
  }

  private applyTransferEvent(previousWalletId: number, transaction: Transaction): void {
    const currentWalletId = this.walletId();

    if (previousWalletId === currentWalletId && transaction.walletId !== currentWalletId) {
      this.transactions.update((current) => current.filter((item) => item.id !== transaction.id));
      return;
    }

    if (transaction.walletId === currentWalletId) {
      this.upsertTransaction(transaction);
    }
  }

  private upsertTransaction(transaction: Transaction): void {
    if (transaction.walletId !== this.walletId()) {
      return;
    }

    this.transactions.update((current) => {
      const withoutExisting = current.filter((item) => item.id !== transaction.id);
      return [transaction, ...withoutExisting];
    });
  }

  private loadTransactions(): void {
    this.transactionService.getTransactions(this.walletId()).subscribe({
      next: (transactions) => this.transactions.set(transactions)
    });
  }

  private buildBogotaDateTime(date: string): string {
    const bogotaTimeParts = new Intl.DateTimeFormat('en-CA', {
      timeZone: 'America/Bogota',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hourCycle: 'h23'
    }).formatToParts(new Date());

    const getPart = (type: string) => bogotaTimeParts.find((part) => part.type === type)?.value ?? '00';
    return `${date}T${getPart('hour')}:${getPart('minute')}:${getPart('second')}`;
  }
}
