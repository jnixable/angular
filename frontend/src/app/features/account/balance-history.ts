import { AccountBalance } from '../../core/models/account.model';
import { Currency } from '../../core/models/currency.model';
import { Transaction } from '../../core/models/transaction.model';

export interface SeriesPoint {
  x: number; // epoch ms
  y: number; // balance in the currency's own units
}

export interface CurrencySeries {
  currency: Currency;
  points: SeriesPoint[];
}

/**
 * Forward balance delta a transaction applies to a given currency, from the
 * perspective of the viewed account.
 */
function deltaFor(tx: Transaction, currency: Currency, viewedAccountNumber: string): number {
  switch (tx.type) {
    case 'DEPOSIT':
      return tx.currencyIn === currency ? (tx.amountIn ?? 0) : 0;
    case 'WITHDRAWAL':
      return tx.currencyOut === currency ? -(tx.amountOut ?? 0) : 0;
    case 'EXCHANGE': {
      let delta = 0;
      if (tx.currencyOut === currency) {
        delta -= tx.amountOut ?? 0;
      }
      if (tx.currencyIn === currency) {
        delta += tx.amountIn ?? 0;
      }
      return delta;
    }
    case 'TRANSFER': {
      const isDestination = tx.accountTo === viewedAccountNumber;
      if (isDestination) {
        return tx.currencyIn === currency ? (tx.amountIn ?? 0) : 0;
      }
      return tx.currencyOut === currency ? -(tx.amountOut ?? 0) : 0;
    }
  }
}

/**
 * Reconstructs a per-currency balance time series for the last 30 days by
 * starting from each current balance and walking the window's transactions in
 * reverse chronological order, undoing each delta. No cross-currency
 * conversion — each currency is its own line in its own units.
 */
export function reconstructBalanceHistory(
  balances: AccountBalance[],
  transactions: Transaction[],
  viewedAccountNumber: string,
  windowStart: number,
  now: number,
): CurrencySeries[] {
  const newestFirst = [...transactions].sort(
    (a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt),
  );

  return balances.map(({ currency, balance }) => {
    let running = balance;
    const points: SeriesPoint[] = [{ x: now, y: running }];

    for (const tx of newestFirst) {
      const t = Date.parse(tx.createdAt);
      // Balance immediately after this transaction.
      points.push({ x: t, y: running });
      // Step back to the balance immediately before this transaction.
      running -= deltaFor(tx, currency, viewedAccountNumber);
    }

    // Anchor the line at the start of the window with the reconstructed balance.
    points.push({ x: windowStart, y: running });
    points.reverse();

    return { currency, points };
  });
}
