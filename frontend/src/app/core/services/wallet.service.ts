import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { Wallet, WalletRequest } from '../models/wallet.model';

@Injectable({ providedIn: 'root' })
export class WalletService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/wallets';

  getWallets(): Observable<Wallet[]> {
    return this.http.get<Wallet[]>(this.apiUrl);
  }

  createWallet(req: WalletRequest): Observable<Wallet> {
    return this.http.post<Wallet>(this.apiUrl, req);
  }

  deleteWallet(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
