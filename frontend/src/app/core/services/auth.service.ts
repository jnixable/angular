import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

export interface LoginResponse {
  token: string;
  expiresInSeconds: number;
}

interface WhoAmIResponse {
  pCode: string;
  firstName: string;
  lastName: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/user`;

  login(pcode: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, { pcode, password });
  }

  whoami(token: string): Observable<User> {
    return this.http
      .get<WhoAmIResponse>(`${this.baseUrl}/whoami`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      .pipe(
        map((res) => ({
          pCode: res.pCode,
          firstName: res.firstName,
          lastName: res.lastName,
          email: res.email,
        })),
      );
  }
}
