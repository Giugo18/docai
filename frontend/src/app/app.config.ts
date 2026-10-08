import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideKeycloak } from 'keycloak-angular';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideKeycloak({
      config: {
        url: 'http://localhost:8180',
        realm: 'docai',
        clientId: 'docai-frontend',
      },
      initOptions: {
        onLoad: 'login-required',   // senza utente → redirect subito al login di Keycloak
        pkceMethod: 'S256',         // PKCE con SHA-256
      },
    }),
  ]
};
