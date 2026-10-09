import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import Keycloak from 'keycloak-js';

export const soloAdmin: CanActivateFn = () => {
  const keycloak = inject(Keycloak);
  const router = inject(Router);
  return keycloak.hasRealmRole('admin') ? true : router.parseUrl('/documenti');
};