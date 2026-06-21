import { Currency } from './currency.model';

export interface Account {
  id: string;
  name: string | null;
  number: string;
  currency: Currency;
  balance: number;
}
