import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import Keycloak from 'keycloak-js';

/** Le rotte protette: senza login si va a Keycloak, e poi si torna esattamente alla pagina richiesta */
export const soloAutenticati: CanActivateFn = async (_route, state) => {
  const keycloak = inject(Keycloak);
  if (keycloak.authenticated) {
    return true;
  }
  await keycloak.login({ redirectUri: window.location.origin + state.url });
  return false;
};