import { createActionGroup, emptyProps, props } from '@ngrx/store';

import { User } from '../../core/models/user.model';

export const AuthActions = createActionGroup({
  source: 'Auth',
  events: {
    Login: props<{ pcode: string; password: string }>(),
    'Login Success': props<{ user: User; token: string }>(),
    'Login Failure': props<{ error: string }>(),
    Logout: emptyProps(),
  },
});
