import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { Transaction, TransactionType } from '../../core/models/transaction.model';
import { Wallet, WalletType } from '../../core/models/wallet.model';
import { AuthService } from '../../core/services/auth.service';
import { TransactionMovementEvent, TransactionService } from '../../core/services/transaction.service';
import { WalletService } from '../../core/services/wallet.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;
  let transactionServiceMock: {
    movements$: Subject<TransactionMovementEvent>;
    getTransactions: jest.Mock;
  };
  let walletServiceMock: {
    getWallets: jest.Mock;
    createWallet: jest.Mock;
    deleteWallet: jest.Mock;
  };
  let authServiceMock: {
    currentUser: ReturnType<typeof import('@angular/core').signal>;
    logout: jest.Mock;
  };
  let routerMock: { navigateByUrl: jest.Mock };

  const wallets: Wallet[] = [
    { id: 1, name: 'Ahorros', type: WalletType.AHORROS, balance: 1000, transactionCount: 2 },
    { id: 2, name: 'Gastos', type: WalletType.GASTOS, balance: 500, transactionCount: 1 }
  ];

  const recentA: Transaction = {
    id: 10,
    amount: 100,
    type: TransactionType.INGRESO,
    category: 'Salary',
    description: null,
    date: '2026-04-24T12:00:00',
    walletId: 1,
    walletName: 'Ahorros'
  };

  const recentB: Transaction = {
    id: 11,
    amount: 30,
    type: TransactionType.GASTO,
    category: 'Food',
    description: null,
    date: '2026-04-24T08:00:00',
    walletId: 2,
    walletName: 'Gastos'
  };

  beforeEach(async () => {
    transactionServiceMock = {
      movements$: new Subject<TransactionMovementEvent>(),
      getTransactions: jest.fn((walletId: number) => (walletId === 1 ? of([recentA]) : of([recentB])))
    };

    walletServiceMock = {
      getWallets: jest.fn().mockReturnValue(of(wallets)),
      createWallet: jest.fn(),
      deleteWallet: jest.fn()
    };

    authServiceMock = {
      currentUser: (await import('@angular/core')).signal({ id: 1, email: 'u@nano.com', name: 'Nano User' }),
      logout: jest.fn()
    };

    routerMock = { navigateByUrl: jest.fn().mockResolvedValue(true) };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: TransactionService, useValue: transactionServiceMock as unknown as TransactionService },
        { provide: WalletService, useValue: walletServiceMock as unknown as WalletService },
        { provide: AuthService, useValue: authServiceMock as unknown as AuthService },
        { provide: Router, useValue: routerMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load wallets and recent movements on init', () => {
    expect(walletServiceMock.getWallets).toHaveBeenCalled();
    expect(component.wallets()).toEqual(wallets);
    expect(component.recentMovements().map((tx) => tx.id)).toEqual([10, 11]);
    expect(component.totalBalance()).toBe(1500);
    expect(component.averageBalance()).toBe(750);
    expect(component.transactionCount()).toBe(3);
    expect(component.userInitial()).toBe('N');
  });

  it('should remove and restore wallet when backend delete fails', () => {
    walletServiceMock.deleteWallet.mockReturnValue(throwError(() => ({ error: { message: 'No permitido' } })));

    component.onWalletDeleted(1);

    expect(component.wallets().map((w) => w.id)).toEqual([1, 2]);
    expect(component.error()).toBe('No permitido');
  });

  it('should delete wallet successfully', () => {
    walletServiceMock.deleteWallet.mockReturnValue(of(void 0));

    component.onWalletDeleted(2);

    expect(component.wallets().map((w) => w.id)).toEqual([1]);
    expect(component.error()).toBeNull();
  });

  it('should validate wallet data before creating a wallet', () => {
    component.newWalletName.set('   ');

    component.createWallet();

    expect(walletServiceMock.createWallet).not.toHaveBeenCalled();
    expect(component.createError()).toBe('Selecciona un tipo de billetera válido.');
  });

  it('should create wallet and reset form', () => {
    const created: Wallet = { id: 3, name: 'Nueva', type: WalletType.INVERSION, balance: 0, transactionCount: 0 };
    walletServiceMock.createWallet.mockReturnValue(of(created));
    component.newWalletName.set('  Nueva  ');
    component.newWalletType.set(WalletType.INVERSION);

    component.createWallet();

    expect(walletServiceMock.createWallet).toHaveBeenCalledWith({ name: 'Nueva', type: WalletType.INVERSION });
    expect(component.wallets()[0].id).toBe(3);
    expect(component.newWalletName()).toBe('');
    expect(component.newWalletType()).toBe(WalletType.AHORROS);
  });

  it('should set error when create wallet fails', () => {
    walletServiceMock.createWallet.mockReturnValue(throwError(() => new Error('failed')));
    component.newWalletName.set('Nueva');

    component.createWallet();

    expect(component.createError()).toBe('No se pudo crear la billetera.');
    expect(component.creatingWallet()).toBe(false);
  });

  it('should apply movement events to recent list', () => {
    component.recentMovements.set([recentA, recentB]);
    const created: Transaction = {
      ...recentA,
      id: 20,
      date: '2026-04-24T13:00:00'
    };

    transactionServiceMock.movements$.next({ type: 'created', transaction: created });
    transactionServiceMock.movements$.next({ type: 'deleted', transactionId: 11 });

    expect(component.recentMovements().map((tx) => tx.id)).toEqual([20, 10]);
  });

  it('should logout and navigate to login', () => {
    component.logout();

    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('should map wallet type labels', () => {
    expect(component.walletTypeLabel(WalletType.AHORROS)).toBe('Ahorros');
    expect(component.walletTypeLabel(WalletType.GASTOS)).toBe('Gastos');
    expect(component.walletTypeLabel(WalletType.INVERSION)).toBe('Inversión');
  });
});
