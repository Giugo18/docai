import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  provideKeycloak,
  includeBearerTokenInterceptor,
  createInterceptorCondition,
  IncludeBearerTokenCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
} from 'keycloak-angular';


/** Configurazione letta da /config.json all'avvio: cambia per ambiente, l'immagine resta la stessa */
export interface ConfigApp {
  keycloakUrl: string;
}

// Il token va aggiunto SOLO alle chiamate verso il nostro backend
const soloApi = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: /^\/api(\/.*)?$/i,
  bearerPrefix: 'Bearer',
});

export function creaAppConfig(config: ConfigApp): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes, withComponentInputBinding()),
      provideKeycloak({
        config: {
          url: config.keycloakUrl,
          realm: 'docai',
          clientId: 'docai-frontend',
        },
        initOptions: {
          onLoad: 'login-required',   // senza utente → redirect subito al login di Keycloak
          pkceMethod: 'S256',         // PKCE con SHA-256
          checkLoginIframe: false,    // niente iframe di controllo sessione: i browser bloccano i cookie di terze parti
        },
      }),
      provideHttpClient(withInterceptors([includeBearerTokenInterceptor])),
      { provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, useValue: [soloApi] },
    ],
  };
}