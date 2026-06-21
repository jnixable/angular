import { Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';

import { AuthActions } from '../../store/auth/auth.actions';
import { selectError, selectLoading } from '../../store/auth/auth.reducer';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(Store);

  protected readonly loading = toSignal(this.store.select(selectLoading), { initialValue: false });
  protected readonly error = toSignal(this.store.select(selectError), { initialValue: null });

  protected readonly form = this.fb.nonNullable.group({
    pcode: ['', Validators.required],
    password: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { pcode, password } = this.form.getRawValue();
    this.store.dispatch(AuthActions.login({ pcode, password }));
  }
}
