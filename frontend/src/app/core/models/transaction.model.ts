export enum TransactionType {
  INGRESO = 'INGRESO',
  GASTO = 'GASTO'
}

export interface Transaction {
  id: number;
  amount: number;
  type: TransactionType;
  category: string;
  description: string | null;
  date: string;
  walletId: number;
  walletName: string;
}

export interface TransactionRequest {
  amount: number;
  type: TransactionType;
  category: string;
  description?: string;
  walletId: number;
  date?: string;
}

export interface TransferRequest {
  targetWalletId: number;
}
