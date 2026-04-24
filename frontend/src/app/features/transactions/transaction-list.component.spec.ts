import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';

import { Transaction, TransactionType } from '../../core/models/transaction.model';
import { TransactionMovementEvent } from '../../core/services/transaction.service';
import { TransactionService } from '../../core/services/transaction.service';
import { TransactionListComponent } from './transaction-list.component';

describe('TransactionListComponent', () => {
  let fixture: ComponentFixture<TransactionListComponent>;
  let component: TransactionListComponent;
  let transactionServiceSpy: {
    getTransactions: jest.Mock;
    createTransaction: jest.Mock;
    transferTransaction: jest.Mock;
    deleteTransaction: jest.Mock;
    movements$: Subject<TransactionMovementEvent>;
  };

  const tx1: Transaction = {
    id: 1,
    amount: 100,
    type: TransactionType.INGRESO,
    category: 'Salary',
    description: null,
    date: '2026-04-20T10:00:00',
    walletId: 1,
    walletName: 'Wallet A'
  };

  const tx2: Transaction = {
    id: 2,
    amount: 30,
    type: TransactionType.GASTO,
    category: 'Food',
    description: null,
    date: '2026-04-22T10:00:00',
    walletId: 1,
    walletName: 'Wallet A'
  };

  beforeEach(async () => {
    transactionServiceSpy = {
      getTransactions: jest.fn(),
      createTransaction: jest.fn(),
      transferTransaction: jest.fn(),
      deleteTransaction: jest.fn(),
      movements$: new Subject<TransactionMovementEvent>()
    };
    transactionServiceSpy.getTransactions.mockReturnValue(of([]));
    transactionServiceSpy.transferTransaction.mockReturnValue(of(tx2));

    await TestBed.configureTestingModule({
      imports: [TransactionListComponent],
      providers: [{ provide: TransactionService, useValue: transactionServiceSpy as unknown as TransactionService }]
    }).compileComponents();

    fixture = TestBed.createComponent(TransactionListComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('walletId', 1);
    fixture.componentRef.setInput('walletIds', [1, 2, 3]);
    fixture.detectChanges();
  });

  it('should filter transactions by category when categoryFilter signal changes', () => {
    component.transactions.set([tx1, tx2]);

    component.categoryFilter.set('food');

    expect(component.filteredTransactions().length).toBe(1);
    expect(component.filteredTransactions()[0].id).toBe(2);
  });

  it('should create transaction and prepend it to the list', () => {
    component.transactions.set([tx1]);
    transactionServiceSpy.createTransaction.mockReturnValue(of(tx2));
    component.newType.set(TransactionType.GASTO);
    component.newAmount.set('30');
    component.newCategory.set('Food');
    component.newDescription.set('Compra en supermercado');
    component.newDate.set('2026-04-22');

    component.createTransaction();
    transactionServiceSpy.movements$.next({ type: 'created', transaction: tx2 });

    expect(transactionServiceSpy.createTransaction).toHaveBeenCalledWith(expect.objectContaining({
      walletId: 1,
      type: TransactionType.GASTO,
      amount: 30,
      category: 'Food',
      description: 'Compra en supermercado',
      date: expect.stringMatching(/^2026-04-22T\d{2}:\d{2}:\d{2}$/)
    }));
    expect(component.transactions()[0].id).toBe(2);
    expect(component.newCategory()).toBe('');
    expect(component.newDescription()).toBe('');
  });

  it('should expose error when create transaction fails', () => {
    transactionServiceSpy.createTransaction.mockReturnValue(throwError(() => new Error('create failed')));
    component.newAmount.set('20');
    component.newCategory.set('Transport');

    component.createTransaction();

    expect(component.createError()).toBe('No se pudo crear la transacción.');
    expect(component.creatingTransaction()).toBe(false);
  });

  it('should filter transactions by date range', () => {
    component.transactions.set([tx1, tx2]);
    component.dateFromFilter.set('2026-04-21');
    component.dateToFilter.set('2026-04-23');

    const filtered = component.filteredTransactions();

    expect(filtered.length).toBe(1);
    expect(filtered[0].id).toBe(2);
  });

  it('should call transferTransaction when drop event occurs between different containers', () => {
    component.transactions.set([tx1]);
    const movingTx: Transaction = { ...tx2, walletId: 2, walletName: 'Wallet B' };
    const event = {
      previousContainer: { data: [movingTx] },
      container: { data: component.filteredTransactions() }
    } as unknown as CdkDragDrop<Transaction[]>;
    Object.assign(event, { previousIndex: 0, currentIndex: 0 });

    component.drop(event);

    expect(transactionServiceSpy.transferTransaction).toHaveBeenCalledWith(2, 1, 2);
  });

  it('should NOT call transferTransaction when drop is in same container', () => {
    component.transactions.set([tx1, tx2]);
    const sameContainer = { data: component.filteredTransactions() };
    const event = {
      previousContainer: sameContainer,
      container: sameContainer
    } as unknown as CdkDragDrop<Transaction[]>;
    Object.assign(event, { previousIndex: 0, currentIndex: 1 });

    component.drop(event);

    expect(transactionServiceSpy.transferTransaction).not.toHaveBeenCalled();
  });

  it('should revert optimistic update if transfer fails', () => {
    component.transactions.set([tx1]);
    const movingTx: Transaction = { ...tx2, walletId: 2, walletName: 'Wallet B' };
    transactionServiceSpy.transferTransaction.mockReturnValue(
      throwError(() => new Error('transfer failed'))
    );
    const event = {
      previousContainer: { data: [movingTx] },
      container: { data: component.filteredTransactions() }
    } as unknown as CdkDragDrop<Transaction[]>;
    Object.assign(event, { previousIndex: 0, currentIndex: 0 });

    component.drop(event);

    expect(component.transactions()).toEqual([tx1]);
  });

  it('should remove transaction when it is transferred out by another list', () => {
    component.transactions.set([tx1, tx2]);
    const transferredTx: Transaction = { ...tx2, walletId: 2, walletName: 'Wallet B' };

    transactionServiceSpy.movements$.next({
      type: 'transferred',
      previousWalletId: 1,
      transaction: transferredTx
    });

    expect(component.transactions()).toEqual([tx1]);
  });
});
