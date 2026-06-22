export type UserType = 'Person' | 'Entity';

export interface PersonDetails {
  firstName: string;
  lastName: string;
  birthday: string | null;
  nationality: string | null;
}

export interface EntityDetails {
  companyName: string;
}

interface UserBase {
  code: string;
  email: string;
}

export interface PersonUser extends UserBase {
  userType: 'Person';
  details: PersonDetails;
}

export interface EntityUser extends UserBase {
  userType: 'Entity';
  details: EntityDetails;
}

export type User = PersonUser | EntityUser;

export function userDisplayName(user: User): string {
  return user.userType === 'Person'
    ? `${user.details.firstName} ${user.details.lastName}`.trim()
    : user.details.companyName;
}
