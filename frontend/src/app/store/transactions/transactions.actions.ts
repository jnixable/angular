import { createActionGroup, props } from '@ngrx/store';

import { Transaction, TransactionPage } from '../../core/models/transaction.model';

export const TransactionsActions = createActionGroup({
  source: 'Transactions',
  events: {
    'Load First Page': props<{
      accountNumber: string;
      size: number;
      from?: string | null;
      to?: string | null;
    }>(),
    'Load Next Page': props<{ accountNumber: string }>(),
    'Load Page Success': props<{ accountNumber: string; page: TransactionPage }>(),
    'Load Page Failure': props<{ accountNumber: string; error: string }>(),
    'Reset Account': props<{ accountNumber: string }>(),
    'Load Chart Range': props<{ accountNumber: string; from: string; to: string }>(),
    'Load Chart Range Success': props<{ accountNumber: string; items: Transaction[] }>(),
    'Load Chart Range Failure': props<{ accountNumber: string; error: string }>(),
    'Load Transaction': props<{ accountNumber: string; id: string }>(),
    'Load Transaction Success': props<{ tx: Transaction }>(),
    'Load Transaction Failure': props<{ error: string }>(),
  },
});
