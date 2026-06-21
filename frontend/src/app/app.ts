import { Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Store } from '@ngrx/store';

import { AuthActions } from './store/auth/auth.actions';
import { selectUser } from './store/auth/auth.reducer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  private readonly store = inject(Store);
  protected readonly title = signal('Swed Bank App');
  protected readonly user = toSignal(this.store.select(selectUser), { initialValue: null });

  protected logout(): void {
    this.store.dispatch(AuthActions.logout());
  }
}
