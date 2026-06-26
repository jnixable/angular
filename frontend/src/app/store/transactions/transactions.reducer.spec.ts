import { Action } from '@ngrx/store';
import { Transaction, TransactionPage } from '../../core/models/transaction.model';
import { TransactionsActions } from './transactions.actions';
import { transactionsFeature } from './transactions.reducer';

const { reducer, name } = transactionsFeature;
const ACC = 'ACC-1';

function initialState() {
  return reducer(undefined, { type: '@@init' } as Action);
}

function tx(id: string): Transaction {
  return {
    id,
    type: 'DEPOSIT',
    amountIn: 10,
    currencyIn: 'EUR',
    amountOut: null,
    currencyOut: null,
    createdAt: '2025-01-01T00:00:00.000Z',
    accountFrom: null,
    accountTo: null,
  };
}

function page(content: Transaction[], number: number, totalPages: number): TransactionPage {
  return {
    content,
    page: { size: 20, number, totalElements: totalPages * 20, totalPages },
  };
}

describe('transactions reducer — pagination', () => {
  it('replaces items for the first page and tracks the filter', () => {
    let state = reducer(
      initialState(),
      TransactionsActions.loadFirstPage({ accountNumber: ACC, size: 20, from: '2025-01-01', to: null }),
    );
    expect(state.lists[ACC].loading).toBe(true);
    expect(state.lists[ACC].filter).toEqual({ from: '2025-01-01', to: null });

    state = reducer(
      state,
      TransactionsActions.loadPageSuccess({ accountNumber: ACC, page: page([tx('a'), tx('b')], 0, 3) }),
    );
    expect(state.lists[ACC].items.map((t) => t.id)).toEqual(['a', 'b']);
    expect(state.lists[ACC].exhausted).toBe(false);
    expect(state.lists[ACC].loading).toBe(false);
  });

  it('appends items for subsequent pages and flags exhaustion on the last page', () => {
    let state = reducer(
      initialState(),
      TransactionsActions.loadFirstPage({ accountNumber: ACC, size: 20 }),
    );
    state = reducer(
      state,
      TransactionsActions.loadPageSuccess({ accountNumber: ACC, page: page([tx('a')], 0, 2) }),
    );
    state = reducer(state, TransactionsActions.loadNextPage({ accountNumber: ACC }));
    state = reducer(
      state,
      TransactionsActions.loadPageSuccess({ accountNumber: ACC, page: page([tx('b')], 1, 2) }),
    );

    expect(state.lists[ACC].items.map((t) => t.id)).toEqual(['a', 'b']);
    expect(state.lists[ACC].exhausted).toBe(true);
  });

  it('isolates state per account', () => {
    let state = reducer(
      initialState(),
      TransactionsActions.loadPageSuccess({ accountNumber: ACC, page: page([tx('a')], 0, 1) }),
    );
    state = reducer(
      state,
      TransactionsActions.loadPageSuccess({ accountNumber: 'ACC-2', page: page([tx('z')], 0, 1) }),
    );
    expect(state.lists[ACC].items.map((t) => t.id)).toEqual(['a']);
    expect(state.lists['ACC-2'].items.map((t) => t.id)).toEqual(['z']);
  });
});

describe('transactions reducer — chart and selected slices', () => {
  it('stores chart items on success', () => {
    const items = [tx('a'), tx('b')];
    const state = reducer(
      initialState(),
      TransactionsActions.loadChartRangeSuccess({ accountNumber: ACC, items }),
    );
    expect(state.charts[ACC].items).toEqual(items);
    expect(state.charts[ACC].loading).toBe(false);
  });

  it('tracks the selected transaction lifecycle', () => {
    let state = reducer(
      initialState(),
      TransactionsActions.loadTransaction({ accountNumber: ACC, id: 'a' }),
    );
    expect(state.selected).toEqual({ tx: null, loading: true, error: null });

    state = reducer(state, TransactionsActions.loadTransactionSuccess({ tx: tx('a') }));
    expect(state.selected.tx?.id).toBe('a');
    expect(state.selected.loading).toBe(false);
  });
});

describe('transactions selectors', () => {
  it('selectHasMore reflects exhaustion', () => {
    let state = reducer(
      initialState(),
      TransactionsActions.loadPageSuccess({ accountNumber: ACC, page: page([tx('a')], 0, 1) }),
    );
    const root = { [name]: state } as { [key: string]: any };
    expect(transactionsFeature.selectHasMore(ACC)(root)).toBe(false);

    state = reducer(
      initialState(),
      TransactionsActions.loadPageSuccess({ accountNumber: ACC, page: page([tx('a')], 0, 3) }),
    );
    expect(transactionsFeature.selectHasMore(ACC)({ [name]: state } as { [key: string]: any })).toBe(true);
  });
});
