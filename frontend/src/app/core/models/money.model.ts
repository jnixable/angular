import { Currency } from './currency.model';

export interface DepositRequest {
  accountNumber: string;
  amount: number;
}

export interface DepositResult {
  accountNumber: string;
  currency: Currency;
  balance: number;
  amount: number;
}

export interface WithdrawRequest {
  accountNumber: string;
  amount: number;
}

export interface WithdrawResult {
  accountNumber: string;
  balance: number;
  amount: number;
}

export interface ExchangeRequest {
  accountNumber: string;
  fromCurrency: Currency;
  toCurrency: Currency;
  amount: number;
}

export interface ExchangeResult {
  accountNumber: string;
  fromCurrency: Currency;
  fromBalance: number;
  toCurrency: Currency;
  toBalance: number;
  debitedAmount: number;
  creditedAmount: number;
  rate: number;
}

export interface TransferRequest {
  sourceAccountNumber: string;
  destinationAccountNumber: string;
  currency: Currency;
  amount: number;
}

export interface TransferResult {
  sourceAccountNumber: string;
  destinationAccountNumber: string;
  currency: Currency;
  amount: number;
  sourceBalance: number;
}
