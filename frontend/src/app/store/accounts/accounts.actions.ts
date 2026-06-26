import { createActionGroup, emptyProps, props } from '@ngrx/store';

import { Account } from '../../core/models/account.model';
import {
  DepositRequest,
  ExchangeRequest,
  TransferRequest,
  WithdrawRequest,
} from '../../core/models/money.model';

export const AccountsActions = createActionGroup({
  source: 'Accounts',
  events: {
    'Load Accounts': emptyProps(),
    'Load Accounts Success': props<{ accounts: Account[] }>(),
    'Load Accounts Failure': props<{ error: string }>(),
    Deposit: props<DepositRequest>(),
    Withdraw: props<WithdrawRequest>(),
    Exchange: props<ExchangeRequest>(),
    Transfer: props<TransferRequest>(),
    'Money Op Success': props<{ accountNumber: string; message: string }>(),
    'Money Op Failure': props<{ error: string }>(),
    'Clear Money Feedback': emptyProps(),
  },
});
