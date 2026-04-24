import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { tap } from 'rxjs/operators';

import { Transaction, TransactionRequest } from '../models/transaction.model';

type TransactionFilters = {
  category?: string;
  dateFrom?: string;
  dateTo?: string;
};

export type TransactionTransferEvent = {
  previousWalletId: number;
  transaction: Transaction;
};

export type TransactionMovementEvent =
  | { type: 'created'; transaction: Transaction }
  | { type: 'transferred'; previousWalletId: number; transaction: Transaction }
  | { type: 'deleted'; transactionId: number; walletId?: number };

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/transactions';
  private readonly movementSubject = new Subject<TransactionMovementEvent>();

  readonly movements$ = this.movementSubject.asObservable();
  readonly transfers$ = new Observable<TransactionTransferEvent>((subscriber) => {
    const subscription = this.movements$.subscribe((event) => {
      if (event.type === 'transferred') {
        subscriber.next({
          previousWalletId: event.previousWalletId,
          transaction: event.transaction
        });
      }
    });

    return () => subscription.unsubscribe();
  });

  getTransactions(walletId: number, filters?: TransactionFilters): Observable<Transaction[]> {
    let params = new HttpParams().set('walletId', walletId);

    if (filters?.category) {
      params = params.set('category', filters.category);
    }
    if (filters?.dateFrom) {
      params = params.set('dateFrom', filters.dateFrom);
    }
    if (filters?.dateTo) {
      params = params.set('dateTo', filters.dateTo);
    }

    return this.http.get<Transaction[]>(this.apiUrl, { params });
  }

  createTransaction(req: TransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.apiUrl, req).pipe(
      tap((transaction) => this.movementSubject.next({ type: 'created', transaction }))
    );
  }

  transferTransaction(id: number, targetWalletId: number, previousWalletId?: number): Observable<Transaction> {
    const url = `${this.apiUrl}/${id}/transfer`;
    const body = { targetWalletId };

    console.log('[TransactionService] solicitud PATCH de transferencia', { url, id, previousWalletId, body });

    return this.http.patch<Transaction>(url, body).pipe(
      tap({
        next: (transaction) => {
          console.log('[TransactionService] transferencia PATCH exitosa', transaction);

          if (previousWalletId !== undefined) {
            this.movementSubject.next({ type: 'transferred', previousWalletId, transaction });
          }
        },
        error: (error) => console.error('[TransactionService] error en transferencia PATCH', {
          status: error?.status,
          statusText: error?.statusText,
          error: error?.error,
          message: error?.message
        })
      })
    );
  }

  deleteTransaction(id: number, walletId?: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.movementSubject.next({ type: 'deleted', transactionId: id, walletId }))
    );
  }
}
