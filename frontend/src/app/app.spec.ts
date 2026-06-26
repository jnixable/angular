import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideMockStore } from '@ngrx/store/testing';

import { App } from './app';
import { selectUser } from './store/auth/auth.reducer';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideMockStore({ selectors: [{ selector: selectUser, value: null }] }),
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
