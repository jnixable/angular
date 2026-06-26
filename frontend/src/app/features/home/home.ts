import { CurrencyPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';

import { Account } from '../../core/models/account.model';
import { AccountsActions } from '../../store/accounts/accounts.actions';
import { accountsFeature } from '../../store/accounts/accounts.reducer';

@Component({
  selector: 'app-home',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class HomePage {
  private readonly store = inject(Store);

  protected readonly accounts = toSignal(this.store.select(accountsFeature.selectAccounts), {
    initialValue: [] as Account[],
  });
  protected readonly loading = toSignal(this.store.select(accountsFeature.selectLoading), {
    initialValue: false,
  });
  protected readonly error = toSignal(this.store.select(accountsFeature.selectError), {
    initialValue: null,
  });

  constructor() {
    this.store.dispatch(AccountsActions.loadAccounts());
  }
}
