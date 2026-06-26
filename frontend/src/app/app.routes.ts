import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'home' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login').then((m) => m.LoginPage),
  },
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./features/home/home').then((m) => m.HomePage),
  },
  {
    path: 'accounts/:accountNumber',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/account/account-overview').then((m) => m.AccountOverviewPage),
  },
  {
    path: 'accounts/:accountNumber/transactions/:transactionId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/transaction/transaction-overview').then((m) => m.TransactionOverviewPage),
  },
  { path: '**', redirectTo: 'home' },
];
