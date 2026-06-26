import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { Store } from '@ngrx/store';

import { AccountsActions } from '../../store/accounts/accounts.actions';
import { accountsFeature } from '../../store/accounts/accounts.reducer';
import { BalanceChartComponent } from './balance-chart';
import { MoneyActionsComponent } from './money-actions';
import { TransactionListComponent } from './transaction-list';

@Component({
  selector: 'app-account-overview',
  imports: [CurrencyPipe, BalanceChartComponent, MoneyActionsComponent, TransactionListComponent],
  templateUrl: './account-overview.html',
  styleUrl: './account-overview.scss',
})
export class AccountOverviewPage {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(Store);

  protected readonly accountNumber = this.route.snapshot.paramMap.get('accountNumber') ?? '';

  protected readonly account = toSignal(
    this.store.select(accountsFeature.selectAccountByNumber(this.accountNumber)),
    { initialValue: null },
  );
  protected readonly accountsLoading = toSignal(this.store.select(accountsFeature.selectLoading), {
    initialValue: false,
  });

  constructor() {
    // Ensure the account is available on deep-link / refresh.
    this.store.dispatch(AccountsActions.loadAccounts());
  }
}
