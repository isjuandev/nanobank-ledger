import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { WalletType } from '../models/wallet.model';
import { WalletService } from './wallet.service';

describe('WalletService', () => {
  let service: WalletService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(WalletService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should call GET /api/wallets', () => {
    service.getWallets().subscribe();

    const req = httpMock.expectOne('/api/wallets');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should call POST /api/wallets with correct body', () => {
    const body = { name: 'Ahorros', type: WalletType.AHORROS };

    service.createWallet(body).subscribe();

    const req = httpMock.expectOne('/api/wallets');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({ id: 1, ...body, balance: 0, transactionCount: 0 });
  });

  it('should call DELETE /api/wallets/{id}', () => {
    service.deleteWallet(99).subscribe();

    const req = httpMock.expectOne('/api/wallets/99');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
