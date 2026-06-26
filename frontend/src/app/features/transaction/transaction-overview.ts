import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';

import { toTransactionView } from '../../core/models/transaction.model';
import { PdfService } from '../../core/services/pdf.service';
import { TransactionsActions } from '../../store/transactions/transactions.actions';
import { transactionsFeature } from '../../store/transactions/transactions.reducer';

@Component({
  selector: 'app-transaction-overview',
  imports: [RouterLink, CurrencyPipe, DatePipe],
  templateUrl: './transaction-overview.html',
  styleUrl: './transaction-overview.scss',
})
export class TransactionOverviewPage {
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(Store);
  private readonly pdf = inject(PdfService);

  protected readonly accountNumber = this.route.snapshot.paramMap.get('accountNumber') ?? '';
  private readonly transactionId = this.route.snapshot.paramMap.get('transactionId') ?? '';

  protected readonly tx = toSignal(this.store.select(transactionsFeature.selectSelectedTx), {
    initialValue: null,
  });
  protected readonly loading = toSignal(
    this.store.select(transactionsFeature.selectSelectedLoading),
    { initialValue: false },
  );
  protected readonly error = toSignal(this.store.select(transactionsFeature.selectSelectedError), {
    initialValue: null,
  });

  protected readonly view = computed(() => {
    const current = this.tx();
    return current ? toTransactionView(current, this.accountNumber) : null;
  });

  constructor() {
    this.store.dispatch(
      TransactionsActions.loadTransaction({
        accountNumber: this.accountNumber,
        id: this.transactionId,
      }),
    );
  }

  protected exportPdf(): void {
    const current = this.tx();
    if (current) {
      this.pdf.generateTransactionPdf(current, this.accountNumber);
    }
  }
}
