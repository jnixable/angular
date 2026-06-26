import { inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, mergeMap, of, switchMap } from 'rxjs';

import { ApiService } from '../../core/services/api.service';
import { describeHttpError } from '../../core/util/http-error';
import { TransactionsActions } from '../transactions/transactions.actions';
import { AccountsActions } from './accounts.actions';

const CHART_WINDOW_MS = 30 * 24 * 60 * 60 * 1000; // 30 days

export const loadAccounts$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService)) =>
    actions$.pipe(
      ofType(AccountsActions.loadAccounts),
      switchMap(() =>
        api.getAccounts().pipe(
          map((accounts) => AccountsActions.loadAccountsSuccess({ accounts })),
          catchError((error) =>
            of(
              AccountsActions.loadAccountsFailure({
                error: describeHttpError(error, 'Unable to load your accounts.'),
              }),
            ),
          ),
        ),
      ),
    ),
  { functional: true },
);

export const deposit$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService)) =>
    actions$.pipe(
      ofType(AccountsActions.deposit),
      switchMap(({ accountNumber, amount }) =>
        api.deposit({ accountNumber, amount }).pipe(
          map((result) =>
            AccountsActions.moneyOpSuccess({
              accountNumber: result.accountNumber,
              message: `Deposited ${result.amount} ${result.currency}.`,
            }),
          ),
          catchError((error) =>
            of(
              AccountsActions.moneyOpFailure({
                error: describeHttpError(error, 'Unable to complete the deposit.'),
              }),
            ),
          ),
        ),
      ),
    ),
  { functional: true },
);

export const withdraw$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService)) =>
    actions$.pipe(
      ofType(AccountsActions.withdraw),
      switchMap(({ accountNumber, amount }) =>
        api.withdraw({ accountNumber, amount }).pipe(
          map((result) =>
            AccountsActions.moneyOpSuccess({
              accountNumber: result.accountNumber,
              message: `Withdrew ${result.amount} from account ${result.accountNumber}.`,
            }),
          ),
          catchError((error) =>
            of(
              AccountsActions.moneyOpFailure({
                error: describeHttpError(error, 'Unable to complete the withdrawal.'),
              }),
            ),
          ),
        ),
      ),
    ),
  { functional: true },
);

export const exchange$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService)) =>
    actions$.pipe(
      ofType(AccountsActions.exchange),
      switchMap(({ accountNumber, fromCurrency, toCurrency, amount }) =>
        api.exchange({ accountNumber, fromCurrency, toCurrency, amount }).pipe(
          map((result) =>
            AccountsActions.moneyOpSuccess({
              accountNumber: result.accountNumber,
              message: `Exchanged ${result.debitedAmount} ${result.fromCurrency} to ${result.creditedAmount} ${result.toCurrency}.`,
            }),
          ),
          catchError((error) =>
            of(
              AccountsActions.moneyOpFailure({
                error: describeHttpError(error, 'Unable to complete the exchange.'),
              }),
            ),
          ),
        ),
      ),
    ),
  { functional: true },
);

export const transfer$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService)) =>
    actions$.pipe(
      ofType(AccountsActions.transfer),
      switchMap(({ sourceAccountNumber, destinationAccountNumber, currency, amount }) =>
        api
          .transfer({ sourceAccountNumber, destinationAccountNumber, currency, amount })
          .pipe(
            map((result) =>
              AccountsActions.moneyOpSuccess({
                accountNumber: result.sourceAccountNumber,
                message: `Transferred ${result.amount} ${result.currency} to ${result.destinationAccountNumber}.`,
              }),
            ),
            catchError((error) =>
              of(
                AccountsActions.moneyOpFailure({
                  error: describeHttpError(error, 'Unable to complete the transfer.'),
                }),
              ),
            ),
          ),
      ),
    ),
  { functional: true },
);

export const refreshAfterMoneyOp$ = createEffect(
  (actions$ = inject(Actions)) =>
    actions$.pipe(
      ofType(AccountsActions.moneyOpSuccess),
      mergeMap(({ accountNumber }) => {
        const now = Date.now();
        return [
          AccountsActions.loadAccounts(),
          TransactionsActions.loadFirstPage({ accountNumber, size: 20 }),
          TransactionsActions.loadChartRange({
            accountNumber,
            from: new Date(now - CHART_WINDOW_MS).toISOString(),
            to: new Date(now).toISOString(),
          }),
        ];
      }),
    ),
  { functional: true },
);
