import { createFeature, createReducer, createSelector, on } from '@ngrx/store';

import { Account } from '../../core/models/account.model';
import { AccountsActions } from './accounts.actions';

export interface MoneyMutationState {
  pending: boolean;
  error: string | null;
  success: string | null;
}

export interface AccountsState {
  accounts: Account[];
  loading: boolean;
  error: string | null;
  loaded: boolean;
  mutation: MoneyMutationState;
}

const EMPTY_MUTATION: MoneyMutationState = {
  pending: false,
  error: null,
  success: null,
};

const initialState: AccountsState = {
  accounts: [],
  loading: false,
  error: null,
  loaded: false,
  mutation: EMPTY_MUTATION,
};

export const accountsFeature = createFeature({
  name: 'accounts',
  reducer: createReducer(
    initialState,
    on(AccountsActions.loadAccounts, (state) => ({ ...state, loading: true, error: null })),
    on(AccountsActions.loadAccountsSuccess, (state, { accounts }) => ({
      ...state,
      accounts,
      loading: false,
      loaded: true,
    })),
    on(AccountsActions.loadAccountsFailure, (state, { error }) => ({
      ...state,
      loading: false,
      error,
    })),
    on(
      AccountsActions.deposit,
      AccountsActions.withdraw,
      AccountsActions.exchange,
      AccountsActions.transfer,
      (state) => ({ ...state, mutation: { pending: true, error: null, success: null } }),
    ),
    on(AccountsActions.moneyOpSuccess, (state, { message }) => ({
      ...state,
      mutation: { pending: false, error: null, success: message },
    })),
    on(AccountsActions.moneyOpFailure, (state, { error }) => ({
      ...state,
      mutation: { pending: false, error, success: null },
    })),
    on(AccountsActions.clearMoneyFeedback, (state) => ({ ...state, mutation: EMPTY_MUTATION })),
  ),
  extraSelectors: ({ selectAccounts }) => ({
    selectAccountByNumber: (accountNumber: string) =>
      createSelector(
        selectAccounts,
        (accounts) => accounts.find((account) => account.number === accountNumber) ?? null,
      ),
  }),
});
