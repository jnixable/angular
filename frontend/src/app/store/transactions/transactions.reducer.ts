import { createFeature, createReducer, createSelector, on } from '@ngrx/store';

import { PageMeta, Transaction } from '../../core/models/transaction.model';
import { TransactionsActions } from './transactions.actions';

export interface TransactionFilter {
  from: string | null;
  to: string | null;
}

export interface TransactionListState {
  items: Transaction[];
  pageMeta: PageMeta | null;
  loading: boolean;
  error: string | null;
  exhausted: boolean;
  filter: TransactionFilter;
}

export interface ChartState {
  items: Transaction[];
  loading: boolean;
  error: string | null;
}

export interface SelectedTransactionState {
  tx: Transaction | null;
  loading: boolean;
  error: string | null;
}

export interface TransactionsState {
  lists: Record<string, TransactionListState>;
  charts: Record<string, ChartState>;
  selected: SelectedTransactionState;
}

const EMPTY_LIST: TransactionListState = {
  items: [],
  pageMeta: null,
  loading: false,
  error: null,
  exhausted: false,
  filter: { from: null, to: null },
};

const EMPTY_CHART: ChartState = {
  items: [],
  loading: false,
  error: null,
};

const EMPTY_SELECTED: SelectedTransactionState = {
  tx: null,
  loading: false,
  error: null,
};

const initialState: TransactionsState = { lists: {}, charts: {}, selected: EMPTY_SELECTED };

function patchList(
  state: TransactionsState,
  accountNumber: string,
  patch: Partial<TransactionListState>,
): TransactionsState {
  const current = state.lists[accountNumber] ?? EMPTY_LIST;
  return {
    ...state,
    lists: { ...state.lists, [accountNumber]: { ...current, ...patch } },
  };
}

function patchChart(
  state: TransactionsState,
  accountNumber: string,
  patch: Partial<ChartState>,
): TransactionsState {
  const current = state.charts[accountNumber] ?? EMPTY_CHART;
  return {
    ...state,
    charts: { ...state.charts, [accountNumber]: { ...current, ...patch } },
  };
}

export const transactionsFeature = createFeature({
  name: 'transactions',
  reducer: createReducer(
    initialState,
    on(TransactionsActions.loadFirstPage, (state, { accountNumber, from, to }) =>
      patchList(state, accountNumber, {
        items: [],
        pageMeta: null,
        exhausted: false,
        loading: true,
        error: null,
        filter: { from: from ?? null, to: to ?? null },
      }),
    ),
    on(TransactionsActions.loadNextPage, (state, { accountNumber }) =>
      patchList(state, accountNumber, { loading: true, error: null }),
    ),
    on(TransactionsActions.loadPageSuccess, (state, { accountNumber, page }) => {
      const current = state.lists[accountNumber] ?? EMPTY_LIST;
      const items = page.page.number === 0 ? page.content : [...current.items, ...page.content];
      const exhausted = page.page.number + 1 >= page.page.totalPages;
      return patchList(state, accountNumber, {
        items,
        pageMeta: page.page,
        exhausted,
        loading: false,
        error: null,
      });
    }),
    on(TransactionsActions.loadPageFailure, (state, { accountNumber, error }) =>
      patchList(state, accountNumber, { loading: false, error }),
    ),
    on(TransactionsActions.resetAccount, (state, { accountNumber }) =>
      patchList(state, accountNumber, { ...EMPTY_LIST }),
    ),
    on(TransactionsActions.loadChartRange, (state, { accountNumber }) =>
      patchChart(state, accountNumber, { items: [], loading: true, error: null }),
    ),
    on(TransactionsActions.loadChartRangeSuccess, (state, { accountNumber, items }) =>
      patchChart(state, accountNumber, { items, loading: false, error: null }),
    ),
    on(TransactionsActions.loadChartRangeFailure, (state, { accountNumber, error }) =>
      patchChart(state, accountNumber, { loading: false, error }),
    ),
    on(TransactionsActions.loadTransaction, (state) => ({
      ...state,
      selected: { tx: null, loading: true, error: null },
    })),
    on(TransactionsActions.loadTransactionSuccess, (state, { tx }) => ({
      ...state,
      selected: { tx, loading: false, error: null },
    })),
    on(TransactionsActions.loadTransactionFailure, (state, { error }) => ({
      ...state,
      selected: { tx: null, loading: false, error },
    })),
  ),
  extraSelectors: ({ selectLists, selectCharts, selectSelected }) => {
    const selectListState = (accountNumber: string) =>
      createSelector(selectLists, (lists) => lists[accountNumber] ?? null);
    const selectChartState = (accountNumber: string) =>
      createSelector(selectCharts, (charts) => charts[accountNumber] ?? null);
    return {
      selectListState,
      selectItems: (accountNumber: string) =>
        createSelector(selectListState(accountNumber), (list) => list?.items ?? []),
      selectListLoading: (accountNumber: string) =>
        createSelector(selectListState(accountNumber), (list) => list?.loading ?? false),
      selectListError: (accountNumber: string) =>
        createSelector(selectListState(accountNumber), (list) => list?.error ?? null),
      selectHasMore: (accountNumber: string) =>
        createSelector(selectListState(accountNumber), (list) => (list ? !list.exhausted : true)),
      selectFilter: (accountNumber: string) =>
        createSelector(
          selectListState(accountNumber),
          (list): TransactionFilter => list?.filter ?? { from: null, to: null },
        ),
      selectChartState,
      selectChartItems: (accountNumber: string) =>
        createSelector(selectChartState(accountNumber), (chart) => chart?.items ?? []),
      selectChartLoading: (accountNumber: string) =>
        createSelector(selectChartState(accountNumber), (chart) => chart?.loading ?? false),
      selectChartError: (accountNumber: string) =>
        createSelector(selectChartState(accountNumber), (chart) => chart?.error ?? null),
      selectSelectedTx: createSelector(selectSelected, (selected) => selected.tx),
      selectSelectedLoading: createSelector(selectSelected, (selected) => selected.loading),
      selectSelectedError: createSelector(selectSelected, (selected) => selected.error),
    };
  },
});
