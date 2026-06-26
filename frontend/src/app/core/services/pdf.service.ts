import { Injectable } from '@angular/core';
import jsPDF from 'jspdf';

import { Transaction, toTransactionView } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class PdfService {
  generateTransactionPdf(tx: Transaction, viewedAccountNumber: string): void {
    const doc = new jsPDF();
    const view = toTransactionView(tx, viewedAccountNumber);

    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text('Swed Bank', 14, 20);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'normal');
    doc.text('Transaction receipt', 14, 28);
    doc.setDrawColor(200);
    doc.line(14, 32, 196, 32);

    const rows: [string, string][] = [
      ['Transaction ID', tx.id],
      ['Type', tx.type],
      ['Direction', view.direction],
      ['Account', viewedAccountNumber],
      ['Date', new Date(tx.createdAt).toLocaleString()],
      ['Amount in', formatLeg(tx.amountIn, tx.currencyIn)],
      ['Amount out', formatLeg(tx.amountOut, tx.currencyOut)],
      ['From account', tx.accountFrom ?? '—'],
      ['To account', tx.accountTo ?? '—'],
    ];

    let y = 44;
    doc.setFontSize(11);
    for (const [label, value] of rows) {
      doc.setFont('helvetica', 'bold');
      doc.text(`${label}:`, 14, y);
      doc.setFont('helvetica', 'normal');
      doc.text(value, 70, y);
      y += 9;
    }

    doc.setFontSize(9);
    doc.setTextColor(150);
    doc.text(`Generated ${new Date().toLocaleString()}`, 14, y + 6);

    doc.save(`transaction-${tx.id}.pdf`);
  }
}

function formatLeg(amount: number | null, currency: string | null): string {
  if (amount == null) {
    return '—';
  }
  return `${amount.toFixed(2)} ${currency ?? ''}`.trim();
}
