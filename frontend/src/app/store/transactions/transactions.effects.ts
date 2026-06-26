import { inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { EMPTY, catchError, concatMap, expand, map, of, reduce, switchMap, take } from 'rxjs';

import { ApiService } from '../../core/services/api.service';
import { Transaction } from '../../core/models/transaction.model';
import { describeHttpError } from '../../core/util/http-error';
import { TransactionsActions } from './transactions.actions';
import { transactionsFeature } from './transactions.reducer';

const CHART_PAGE_SIZE = 100;
const CHART_MAX_PAGES = 20;

export const loadFirstPage$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService)) =>
    actions$.pipe(
      ofType(TransactionsActions.loadFirstPage),
      switchMap(({ accountNumber, size, from, to }) =>
        api.getTransactions(accountNumber, { page: 0, size, from, to }).pipe(
          map((page) => TransactionsActions.loadPageSuccess({ accountNumber, page })),
          catchError((error) =>
            of(
              TransactionsActions.loadPageFailure({
                accountNumber,
                error: describeHttpError(error, 'Unable to load transactions.'),
              }),
            ),
          ),
        ),
      ),
    ),
  { functional: true },
);

export const loadNextPage$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService), store = inject(Store)) =>
    actions$.pipe(
      ofType(TransactionsActions.loadNextPage),
      concatMap(({ accountNumber }) =>
        store.select(transactionsFeature.selectListState(accountNumber)).pipe(
          take(1),
          switchMap((list) => {
            const size = list?.pageMeta?.size ?? 20;
            const nextPage = (list?.pageMeta?.number ?? -1) + 1;
            const filter = list?.filter ?? { from: null, to: null };
            return api
              .getTransactions(accountNumber, {
                page: nextPage,
                size,
                from: filter.from,
                to: filter.to,
              })
              .pipe(
                map((page) => TransactionsActions.loadPageSuccess({ accountNumber, page })),
                catchError((error) =>
                  of(
                    TransactionsActions.loadPageFailure({
                      accountNumber,
                      error: describeHttpError(error, 'Unable to load more transactions.'),
                    }),
                  ),
                ),
              );
          }),
        ),
      ),
    ),
  { functional: true },
);

export const loadChartRange$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService)) =>
    actions$.pipe(
      ofType(TransactionsActions.loadChartRange),
      switchMap(({ accountNumber, from, to }) =>
        api.getTransactions(accountNumber, { page: 0, size: CHART_PAGE_SIZE, from, to }).pipe(
          expand((page) => {
            const next = page.page.number + 1;
            return next < page.page.totalPages && next < CHART_MAX_PAGES
              ? api.getTransactions(accountNumber, {
                page: next,
                size: CHART_PAGE_SIZE,
                from,
                to,
              })
              : EMPTY;
          }),
          reduce((acc, page) => acc.concat(page.content), [] as Transaction[]),
          map((items) => TransactionsActions.loadChartRangeSuccess({ accountNumber, items })),
          catchError((error) =>
            of(
              TransactionsActions.loadChartRangeFailure({
                accountNumber,
                error: describeHttpError(error, 'Unable to load the balance chart.'),
              }),
            ),
          ),
        ),
      ),
    ),
  { functional: true },
);

export const loadTransaction$ = createEffect(
  (actions$ = inject(Actions), api = inject(ApiService)) =>
    actions$.pipe(
      ofType(TransactionsActions.loadTransaction),
      switchMap(({ accountNumber, id }) =>
        api.getTransaction(accountNumber, id).pipe(
          map((tx) => TransactionsActions.loadTransactionSuccess({ tx })),
          catchError((error) =>
            of(
              TransactionsActions.loadTransactionFailure({
                error: describeHttpError(error, 'Unable to load this transaction.'),
              }),
            ),
          ),
        ),
      ),
    ),
  { functional: true },
);
