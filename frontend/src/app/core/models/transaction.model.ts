import { Currency } from './currency.model';

export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'EXCHANGE' | 'TRANSFER';

export interface Transaction {
  id: string;
  type: TransactionType;
  amountIn: number | null;
  currencyIn: Currency | null;
  amountOut: number | null;
  currencyOut: Currency | null;
  createdAt: string; // ISO instant
  accountFrom: string | null;
  accountTo: string | null;
}

export type TransactionDirection = 'CREDIT' | 'DEBIT' | 'EXCHANGE';

export interface TransactionView extends Transaction {
  direction: TransactionDirection;
  counterpartyAccountNumber: string | null;
}

export interface PageMeta {
  size: number;
  number: number;
  totalElements: number;
  totalPages: number;
}

export interface TransactionPage {
  content: Transaction[];
  page: PageMeta;
}

export function deriveDirection(
  tx: Transaction,
  viewedAccountNumber: string,
): TransactionDirection {
  switch (tx.type) {
    case 'DEPOSIT':
      return 'CREDIT';
    case 'WITHDRAWAL':
      return 'DEBIT';
    case 'EXCHANGE':
      return 'EXCHANGE';
    case 'TRANSFER':
      return tx.accountTo === viewedAccountNumber ? 'CREDIT' : 'DEBIT';
  }
}

export function toTransactionView(
  tx: Transaction,
  viewedAccountNumber: string,
): TransactionView {
  const direction = deriveDirection(tx, viewedAccountNumber);
  const counterpartyAccountNumber =
    tx.type === 'TRANSFER'
      ? tx.accountTo === viewedAccountNumber
        ? tx.accountFrom
        : tx.accountTo
      : null;
  return { ...tx, direction, counterpartyAccountNumber };
}
