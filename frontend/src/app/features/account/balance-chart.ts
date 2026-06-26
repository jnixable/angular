import { Component, computed, effect, inject, input, untracked } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { switchMap } from 'rxjs';

import { Account } from '../../core/models/account.model';
import { Currency } from '../../core/models/currency.model';
import { Transaction } from '../../core/models/transaction.model';
import { TransactionsActions } from '../../store/transactions/transactions.actions';
import { transactionsFeature } from '../../store/transactions/transactions.reducer';
import { reconstructBalanceHistory } from './balance-history';

const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

const CURRENCY_COLORS: Record<Currency, string> = {
  EUR: '#ff5f00',
  USD: '#1e8e3e',
  SEK: '#1f6feb',
  GBP: '#8e44ad',
};

@Component({
  selector: 'app-balance-chart',
  imports: [BaseChartDirective],
  templateUrl: './balance-chart.html',
  styleUrl: './balance-chart.scss',
})
export class BalanceChartComponent {
  private readonly store = inject(Store);

  readonly account = input.required<Account>();

  private readonly accountNumber = computed(() => this.account().number);
  private readonly accountNumber$ = toObservable(this.accountNumber);

  protected readonly chartItems = toSignal(
    this.accountNumber$.pipe(
      switchMap((acc) => this.store.select(transactionsFeature.selectChartItems(acc))),
    ),
    { initialValue: [] as Transaction[] },
  );
  protected readonly loading = toSignal(
    this.accountNumber$.pipe(
      switchMap((acc) => this.store.select(transactionsFeature.selectChartLoading(acc))),
    ),
    { initialValue: false },
  );
  protected readonly error = toSignal(
    this.accountNumber$.pipe(
      switchMap((acc) => this.store.select(transactionsFeature.selectChartError(acc))),
    ),
    { initialValue: null },
  );

  protected readonly hasActivity = computed(() => this.chartItems().length > 0);

  protected readonly chartData = computed<ChartConfiguration<'line'>['data']>(() => {
    const account = this.account();
    const now = Date.now();
    const windowStart = now - THIRTY_DAYS_MS;
    const series = reconstructBalanceHistory(
      account.balances,
      this.chartItems(),
      account.number,
      windowStart,
      now,
    );
    return {
      datasets: series.map((line) => ({
        label: line.currency,
        data: line.points,
        borderColor: CURRENCY_COLORS[line.currency],
        backgroundColor: CURRENCY_COLORS[line.currency],
        tension: 0.2,
        pointRadius: 2,
        stepped: false,
      })),
    };
  });

  protected readonly chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'nearest', intersect: false },
    scales: {
      x: {
        type: 'linear',
        ticks: {
          maxTicksLimit: 8,
          callback: (value) =>
            new Date(value as number).toLocaleDateString(undefined, {
              month: 'short',
              day: 'numeric',
            }),
        },
      },
      y: {
        ticks: { maxTicksLimit: 6 },
      },
    },
    plugins: {
      legend: { position: 'top' },
      tooltip: {
        callbacks: {
          title: (items) => {
            const x = items[0]?.parsed.x;
            return x != null ? new Date(x).toLocaleString() : '';
          },
        },
      },
    },
  };

  constructor() {
    // Load the fixed last-30-days window whenever the account changes.
    effect(() => {
      const accountNumber = this.accountNumber();
      untracked(() => {
        const now = Date.now();
        this.store.dispatch(
          TransactionsActions.loadChartRange({
            accountNumber,
            from: new Date(now - THIRTY_DAYS_MS).toISOString(),
            to: new Date(now).toISOString(),
          }),
        );
      });
    });
  }
}
