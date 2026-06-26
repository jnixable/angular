import {
  ApplicationConfig,
  isDevMode,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { authFeature } from './store/auth/auth.reducer';
import { accountsFeature } from './store/accounts/accounts.reducer';
import { transactionsFeature } from './store/transactions/transactions.reducer';
import * as authEffects from './store/auth/auth.effects';
import * as accountsEffects from './store/accounts/accounts.effects';
import * as transactionsEffects from './store/transactions/transactions.effects';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    provideStore({
      [authFeature.name]: authFeature.reducer,
      [accountsFeature.name]: accountsFeature.reducer,
      [transactionsFeature.name]: transactionsFeature.reducer,
    }),
    provideEffects(authEffects, accountsEffects, transactionsEffects),
    provideCharts(withDefaultRegisterables()),
    provideStoreDevtools({ maxAge: 25, logOnly: !isDevMode() }),
  ],
};
