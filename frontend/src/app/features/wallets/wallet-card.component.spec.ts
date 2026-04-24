import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WalletType } from '../../core/models/wallet.model';
import { WalletCardComponent } from './wallet-card.component';

describe('WalletCardComponent', () => {
  let fixture: ComponentFixture<WalletCardComponent>;
  let component: WalletCardComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [WalletCardComponent] }).compileComponents();

    fixture = TestBed.createComponent(WalletCardComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('wallet', {
      id: 1,
      name: 'Ahorros',
      type: WalletType.AHORROS,
      balance: 120,
      transactionCount: 3
    });
    fixture.detectChanges();
  });

  it('should emit wallet id when deletion is confirmed', () => {
    jest.spyOn(window, 'confirm').mockReturnValue(true);
    const emitSpy = jest.spyOn(component.walletDeleted, 'emit');

    component.onDelete();

    expect(emitSpy).toHaveBeenCalledWith(1);
  });

  it('should not emit when deletion is canceled', () => {
    jest.spyOn(window, 'confirm').mockReturnValue(false);
    const emitSpy = jest.spyOn(component.walletDeleted, 'emit');

    component.onDelete();

    expect(emitSpy).not.toHaveBeenCalled();
  });

  it('should map wallet labels', () => {
    expect(component.walletTypeLabel(WalletType.AHORROS)).toBe('Ahorros');
    expect(component.walletTypeLabel(WalletType.GASTOS)).toBe('Gastos');
    expect(component.walletTypeLabel(WalletType.INVERSION)).toBe('Inversión');
  });
});
