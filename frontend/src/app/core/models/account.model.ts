import { Currency } from './currency.model';

export interface AccountBalance {
  currency: Currency;
  balance: number;
}

export interface Account {
  number: string;
  name: string | null;
  balances: AccountBalance[];
}
