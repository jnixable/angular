import { CurrencyPipe, DatePipe } from '@angular/common';
import {
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { switchMap } from 'rxjs';

import { Currency } from '../../core/models/currency.model';
import {
  Transaction,
  TransactionDirection,
  TransactionType,
  toTransactionView,
} from '../../core/models/transaction.model';
import { TransactionsActions } from '../../store/transactions/transactions.actions';
import { transactionsFeature } from '../../store/transactions/transactions.reducer';

const PAGE_SIZE = 20;
const LIST_WINDOW_MS = 90 * 24 * 60 * 60 * 1000; // 90 days

interface AmountLeg {
  sign: '+' | '-';
  amount: number;
  currency: Currency;
}

interface TransactionRow {
  id: string;
  createdAt: string;
  type: TransactionType;
  direction: TransactionDirection;
  legs: AmountLeg[];
  counterparty: string | null;
}

@Component({
  selector: 'app-transaction-list',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './transaction-list.html',
  styleUrl: './transaction-list.scss',
})
export class TransactionListComponent {
  private readonly store = inject(Store);

  readonly accountNumber = input.required<string>();

  private readonly accountNumber$ = toObservable(this.accountNumber);

  protected readonly items = toSignal(
    this.accountNumber$.pipe(
      switchMap((acc) => this.store.select(transactionsFeature.selectItems(acc))),
    ),
    { initialValue: [] as Transaction[] },
  );
  protected readonly loading = toSignal(
    this.accountNumber$.pipe(
      switchMap((acc) => this.store.select(transactionsFeature.selectListLoading(acc))),
    ),
    { initialValue: false },
  );
  protected readonly error = toSignal(
    this.accountNumber$.pipe(
      switchMap((acc) => this.store.select(transactionsFeature.selectListError(acc))),
    ),
    { initialValue: null },
  );
  protected readonly hasMore = toSignal(
    this.accountNumber$.pipe(
      switchMap((acc) => this.store.select(transactionsFeature.selectHasMore(acc))),
    ),
    { initialValue: true },
  );

  protected readonly fromDate = signal('');
  protected readonly toDate = signal('');

  protected readonly rows = computed<TransactionRow[]>(() => {
    const viewed = this.accountNumber();
    return this.items().map((tx) => buildRow(tx, viewed));
  });

  protected readonly isEmpty = computed(
    () => !this.loading() && !this.error() && this.items().length === 0,
  );

  private readonly sentinel = viewChild<ElementRef<HTMLElement>>('sentinel');

  constructor() {
    effect(() => {
      const acc = this.accountNumber();
      this.store.dispatch(
        TransactionsActions.loadFirstPage({
          accountNumber: acc,
          size: PAGE_SIZE,
          from: defaultListFrom(),
        }),
      );
    });

    effect((onCleanup) => {
      const el = this.sentinel()?.nativeElement;
      if (!el) {
        return;
      }
      const observer = new IntersectionObserver((entries) => {
        const visible = entries.some((entry) => entry.isIntersecting);
        if (visible && this.hasMore() && !this.loading()) {
          this.store.dispatch(
            TransactionsActions.loadNextPage({ accountNumber: this.accountNumber() }),
          );
        }
      });
      observer.observe(el);
      onCleanup(() => observer.disconnect());
    });
  }

  protected applyFilter(): void {
    this.store.dispatch(
      TransactionsActions.loadFirstPage({
        accountNumber: this.accountNumber(),
        size: PAGE_SIZE,
        from: toStartInstant(this.fromDate()),
        to: toEndInstantExclusive(this.toDate()),
      }),
    );
  }

  protected clearFilter(): void {
    this.fromDate.set('');
    this.toDate.set('');
    this.store.dispatch(
      TransactionsActions.loadFirstPage({
        accountNumber: this.accountNumber(),
        size: PAGE_SIZE,
        from: defaultListFrom(),
      }),
    );
  }

  protected retry(): void {
    this.store.dispatch(TransactionsActions.loadNextPage({ accountNumber: this.accountNumber() }));
  }
}

function defaultListFrom(): string {
  return new Date(Date.now() - LIST_WINDOW_MS).toISOString();
}

function toStartInstant(dateStr: string): string | null {
  return dateStr ? `${dateStr}T00:00:00.000Z` : null;
}

function toEndInstantExclusive(dateStr: string): string | null {
  if (!dateStr) {
    return null;
  }
  const start = new Date(`${dateStr}T00:00:00.000Z`);
  start.setUTCDate(start.getUTCDate() + 1);
  return start.toISOString();
}

function buildRow(tx: Transaction, viewedAccountNumber: string): TransactionRow {
  const view = toTransactionView(tx, viewedAccountNumber);
  const legs: AmountLeg[] = [];

  if (view.direction === 'EXCHANGE') {
    if (tx.amountOut != null && tx.currencyOut) {
      legs.push({ sign: '-', amount: tx.amountOut, currency: tx.currencyOut });
    }
    if (tx.amountIn != null && tx.currencyIn) {
      legs.push({ sign: '+', amount: tx.amountIn, currency: tx.currencyIn });
    }
  } else if (view.direction === 'CREDIT') {
    legs.push({
      sign: '+',
      amount: tx.amountIn ?? tx.amountOut ?? 0,
      currency: tx.currencyIn ?? tx.currencyOut ?? 'EUR',
    });
  } else {
    legs.push({
      sign: '-',
      amount: tx.amountOut ?? tx.amountIn ?? 0,
      currency: tx.currencyOut ?? tx.currencyIn ?? 'EUR',
    });
  }

  return {
    id: tx.id,
    createdAt: tx.createdAt,
    type: tx.type,
    direction: view.direction,
    legs,
    counterparty: view.counterpartyAccountNumber,
  };
}
