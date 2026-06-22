import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

export interface LoginResponse {
  token: string;
  expiresInSeconds: number;
}

interface WhoAmIPersonDetails {
  firstname: string;
  lastname: string;
  birthday: string | null;
  nationality: string | null;
}

interface WhoAmIEntityDetails {
  companyName: string;
}

interface WhoAmIResponse {
  userType: 'Person' | 'Entity';
  code: string;
  userDetails: WhoAmIPersonDetails | WhoAmIEntityDetails;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/user`;

  login(code: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, { code, password });
  }

  whoami(token: string): Observable<User> {
    return this.http
      .get<WhoAmIResponse>(`${this.baseUrl}/whoami`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      .pipe(map(toUser));
  }
}

function toUser(res: WhoAmIResponse): User {
  if (res.userType === 'Person') {
    const details = res.userDetails as WhoAmIPersonDetails;
    return {
      userType: 'Person',
      code: res.code,
      email: res.email,
      details: {
        firstName: details.firstname,
        lastName: details.lastname,
        birthday: details.birthday ?? null,
        nationality: details.nationality ?? null,
      },
    };
  }

  const details = res.userDetails as WhoAmIEntityDetails;
  return {
    userType: 'Entity',
    code: res.code,
    email: res.email,
    details: { companyName: details.companyName },
  };
}
