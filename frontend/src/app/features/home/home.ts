import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';

import { Account } from '../../core/models/account.model';
import { AuthActions } from '../../store/auth/auth.actions';
import { selectUser } from '../../store/auth/auth.reducer';

@Component({
  selector: 'app-home',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class HomePage {
  private readonly store = inject(Store);

  protected readonly user = toSignal(this.store.select(selectUser), { initialValue: null });

  protected readonly accounts: readonly Account[] = [
    { id: 'acc-1', name: 'Salary account', number: 'LV1234567890', currency: 'EUR', balance: 4210.55 },
    { id: 'acc-2', name: 'My Deposit EUR', number: 'LV0987654321', currency: 'EUR', balance: 15750 },
    { id: 'acc-3', name: null, number: 'LV1122334455', currency: 'USD', balance: 980.2 },
    { id: 'acc-4', name: null, number: 'LV5566778899', currency: 'SEK', balance: 32450.75 },
  ];

  protected logout(): void {
    this.store.dispatch(AuthActions.logout());
  }
}
