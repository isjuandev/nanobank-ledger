import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TransactionType } from '../../core/models/transaction.model';
import { TransactionItemComponent } from './transaction-item.component';

describe('TransactionItemComponent', () => {
  let fixture: ComponentFixture<TransactionItemComponent>;
  let component: TransactionItemComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TransactionItemComponent] }).compileComponents();

    fixture = TestBed.createComponent(TransactionItemComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('transaction', {
      id: 1,
      amount: 150,
      type: TransactionType.INGRESO,
      category: 'Salary',
      description: null,
      date: '2026-04-24T10:00:00',
      walletId: 1,
      walletName: 'Main'
    });
    fixture.detectChanges();
  });

  it('should map labels for transaction types', () => {
    expect(component.transactionTypeLabel(TransactionType.INGRESO)).toBe('Ingreso');
    expect(component.transactionTypeLabel(TransactionType.GASTO)).toBe('Gasto');
  });
});
