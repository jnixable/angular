import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Account } from '../models/account.model';
import {
  DepositRequest,
  DepositResult,
  ExchangeRequest,
  ExchangeResult,
  TransferRequest,
  TransferResult,
  WithdrawRequest,
  WithdrawResult,
} from '../models/money.model';
import { Transaction, TransactionPage } from '../models/transaction.model';

export interface TransactionQuery {
  page?: number;
  size?: number;
  from?: string | null;
  to?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getAccounts(): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.baseUrl}/accounts`);
  }

  getAccount(accountNumber: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/accounts/${accountNumber}`);
  }

  getTransactions(
    accountNumber: string,
    query: TransactionQuery = {},
  ): Observable<TransactionPage> {
    let params = new HttpParams();
    if (query.page != null) {
      params = params.set('page', query.page);
    }
    if (query.size != null) {
      params = params.set('size', query.size);
    }
    if (query.from) {
      params = params.set('from', query.from);
    }
    if (query.to) {
      params = params.set('to', query.to);
    }
    return this.http.get<TransactionPage>(
      `${this.baseUrl}/accounts/${accountNumber}/transactions`,
      { params },
    );
  }

  getTransaction(accountNumber: string, transactionId: string): Observable<Transaction> {
    return this.http.get<Transaction>(
      `${this.baseUrl}/accounts/${accountNumber}/transactions/${transactionId}`,
    );
  }

  deposit(request: DepositRequest): Observable<DepositResult> {
    return this.http.post<DepositResult>(`${this.baseUrl}/accounts/deposit`, request);
  }

  withdraw(request: WithdrawRequest): Observable<WithdrawResult> {
    return this.http.post<WithdrawResult>(`${this.baseUrl}/accounts/withdraw`, request);
  }

  exchange(request: ExchangeRequest): Observable<ExchangeResult> {
    return this.http.post<ExchangeResult>(`${this.baseUrl}/accounts/exchange`, request);
  }

  transfer(request: TransferRequest): Observable<TransferResult> {
    return this.http.post<TransferResult>(`${this.baseUrl}/accounts/transfer`, request);
  }
}
