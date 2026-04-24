import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TransactionType } from '../models/transaction.model';
import { TransactionService } from './transaction.service';

describe('TransactionService', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });

    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    jest.restoreAllMocks();
  });

  it('should build query params with filters', () => {
    service.getTransactions(7, { category: 'Food', dateFrom: '2026-01-01', dateTo: '2026-01-31' }).subscribe();

    const req = httpMock.expectOne((request) =>
      request.url === '/api/transactions' &&
      request.params.get('walletId') === '7' &&
      request.params.get('category') === 'Food' &&
      request.params.get('dateFrom') === '2026-01-01' &&
      request.params.get('dateTo') === '2026-01-31'
    );

    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should emit created movement when transaction is created', () => {
    const events: unknown[] = [];
    service.movements$.subscribe((event) => events.push(event));

    service
      .createTransaction({
        walletId: 1,
        amount: 100,
        category: 'Salary',
        type: TransactionType.INGRESO
      })
      .subscribe();

    const req = httpMock.expectOne('/api/transactions');
    expect(req.request.method).toBe('POST');
    req.flush({
      id: 10,
      walletId: 1,
      walletName: 'Main',
      amount: 100,
      category: 'Salary',
      description: null,
      type: TransactionType.INGRESO,
      date: '2026-04-24T10:00:00'
    });

    expect(events).toEqual([
      expect.objectContaining({ type: 'created', transaction: expect.objectContaining({ id: 10 }) })
    ]);
  });

  it('should emit transfer events only when previous wallet id is provided', () => {
    const transfers: unknown[] = [];
    service.transfers$.subscribe((event) => transfers.push(event));

    service.transferTransaction(55, 2, 1).subscribe();
    const first = httpMock.expectOne('/api/transactions/55/transfer');
    expect(first.request.method).toBe('PATCH');
    expect(first.request.body).toEqual({ targetWalletId: 2 });
    first.flush({
      id: 55,
      walletId: 2,
      walletName: 'Destino',
      amount: 50,
      category: 'Transfer',
      description: null,
      type: TransactionType.GASTO,
      date: '2026-04-24T10:00:00'
    });

    service.transferTransaction(56, 3).subscribe();
    const second = httpMock.expectOne('/api/transactions/56/transfer');
    second.flush({
      id: 56,
      walletId: 3,
      walletName: 'Destino 2',
      amount: 70,
      category: 'Transfer',
      description: null,
      type: TransactionType.GASTO,
      date: '2026-04-24T10:00:00'
    });

    expect(transfers).toHaveLength(1);
    expect(transfers[0]).toEqual(
      expect.objectContaining({ previousWalletId: 1, transaction: expect.objectContaining({ id: 55 }) })
    );
  });

  it('should emit deleted movement when deleting transaction', () => {
    const events: unknown[] = [];
    service.movements$.subscribe((event) => events.push(event));

    service.deleteTransaction(90, 3).subscribe();

    const req = httpMock.expectOne('/api/transactions/90');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    expect(events).toEqual([{ type: 'deleted', transactionId: 90, walletId: 3 }]);
  });
});
