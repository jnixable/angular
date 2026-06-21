import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { map, take } from 'rxjs';

import { selectToken } from '../../store/auth/auth.reducer';

export const authGuard: CanActivateFn = () => {
  const store = inject(Store);
  const router = inject(Router);
  return store.select(selectToken).pipe(
    take(1),
    map((token) => (token ? true : router.createUrlTree(['/login']))),
  );
};
