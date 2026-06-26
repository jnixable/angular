import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Store } from '@ngrx/store';

import { Account } from '../../core/models/account.model';
import { Currency } from '../../core/models/currency.model';
import { AccountsActions } from '../../store/accounts/accounts.actions';
import { accountsFeature } from '../../store/accounts/accounts.reducer';

type MoneyAction = 'deposit' | 'withdraw' | 'exchange' | 'transfer';

const EMPTY_MUTATION = { pending: false, error: null as string | null, success: null as string | null };

export const CURRENCIES: Currency[] = ['EUR', 'USD', 'SEK', 'GBP'];

function differentCurrencies(group: AbstractControl): ValidationErrors | null {
  const from = group.get('fromCurrency')?.value;
  const to = group.get('toCurrency')?.value;
  return from && to && from === to ? { sameCurrency: true } : null;
}

@Component({
  selector: 'app-money-actions',
  imports: [ReactiveFormsModule],
  templateUrl: './money-actions.html',
  styleUrl: './money-actions.scss',
})
export class MoneyActionsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(Store);

  readonly account = input.required<Account>();

  protected readonly currencies = CURRENCIES;
  protected readonly active = signal<MoneyAction | null>(null);

  private readonly mutation = toSignal(this.store.select(accountsFeature.selectMutation), {
    initialValue: EMPTY_MUTATION,
  });
  protected readonly pending = computed(() => this.mutation().pending);
  protected readonly feedbackError = computed(() => this.mutation().error);
  protected readonly feedbackSuccess = computed(() => this.mutation().success);

  protected readonly depositForm = this.fb.group({
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
  });

  protected readonly withdrawForm = this.fb.group({
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
  });

  protected readonly exchangeForm = this.fb.group(
    {
      fromCurrency: this.fb.control<Currency>('EUR', { nonNullable: true }),
      toCurrency: this.fb.control<Currency>('USD', { nonNullable: true }),
      amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
    },
    { validators: differentCurrencies },
  );

  protected readonly transferForm = this.fb.group({
    destinationAccountNumber: this.fb.control('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    currency: this.fb.control<Currency>('EUR', { nonNullable: true }),
    amount: this.fb.control<number | null>(null, [Validators.required, Validators.min(0.01)]),
  });

  constructor() {
    effect(() => {
      if (this.feedbackSuccess()) {
        this.resetForms();
      }
    });
  }

  protected select(action: MoneyAction): void {
    this.active.set(this.active() === action ? null : action);
    this.store.dispatch(AccountsActions.clearMoneyFeedback());
  }

  protected submitDeposit(): void {
    if (this.depositForm.invalid) {
      this.depositForm.markAllAsTouched();
      return;
    }
    this.store.dispatch(
      AccountsActions.deposit({
        accountNumber: this.account().number,
        amount: this.depositForm.controls.amount.value!,
      }),
    );
  }

  protected submitWithdraw(): void {
    if (this.withdrawForm.invalid) {
      this.withdrawForm.markAllAsTouched();
      return;
    }
    this.store.dispatch(
      AccountsActions.withdraw({
        accountNumber: this.account().number,
        amount: this.withdrawForm.controls.amount.value!,
      }),
    );
  }

  protected submitExchange(): void {
    if (this.exchangeForm.invalid) {
      this.exchangeForm.markAllAsTouched();
      return;
    }
    const { fromCurrency, toCurrency, amount } = this.exchangeForm.getRawValue();
    this.store.dispatch(
      AccountsActions.exchange({
        accountNumber: this.account().number,
        fromCurrency,
        toCurrency,
        amount: amount!,
      }),
    );
  }

  protected submitTransfer(): void {
    if (this.transferForm.invalid) {
      this.transferForm.markAllAsTouched();
      return;
    }
    const { destinationAccountNumber, currency, amount } = this.transferForm.getRawValue();
    this.store.dispatch(
      AccountsActions.transfer({
        sourceAccountNumber: this.account().number,
        destinationAccountNumber,
        currency,
        amount: amount!,
      }),
    );
  }

  private resetForms(): void {
    this.depositForm.reset();
    this.withdrawForm.reset();
    this.exchangeForm.reset({ fromCurrency: 'EUR', toCurrency: 'USD', amount: null });
    this.transferForm.reset({ destinationAccountNumber: '', currency: 'EUR', amount: null });
  }
}
