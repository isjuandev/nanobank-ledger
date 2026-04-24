export enum WalletType {
  AHORROS = 'AHORROS',
  GASTOS = 'GASTOS',
  INVERSION = 'INVERSION'
}

export interface Wallet {
  id: number;
  name: string;
  type: WalletType;
  balance: number;
  transactionCount: number;
}

export interface WalletRequest {
  name: string;
  type: WalletType;
}
