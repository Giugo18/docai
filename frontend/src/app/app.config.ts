import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  provideKeycloak,
  includeBearerTokenInterceptor,
  createInterceptorCondition,
  IncludeBearerTokenCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  withAutoRefreshToken,
  AutoRefreshTokenService,
  UserActivityService,
} from 'keycloak-angular';
import { registerLocaleData } from '@angular/common';
import localeIt from '@angular/common/locales/it';
import { sessioneScadutaInterceptor } from './auth/sessione-scaduta.interceptor';


/** Configurazione letta da /config.json all'avvio: cambia per ambiente, l'immagine resta la stessa */
export interface ConfigApp {
  keycloakUrl: string;
}

// Il token va aggiunto SOLO alle chiamate verso il nostro backend
const soloApi = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: /^\/api(\/.*)?$/i,
  bearerPrefix: 'Bearer',
});

registerLocaleData(localeIt); 

export function creaAppConfig(config: ConfigApp): ApplicationConfig {
  return {
    providers: [
      { provide: LOCALE_ID, useValue: 'it' },
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
        features: [
          withAutoRefreshToken({
            onInactivityTimeout: 'logout',          // inattivo → logout
            sessionTimeout: 30 * 60 * 1000,         // 30 minuti senza attività
          }),
        ],
        providers: [AutoRefreshTokenService, UserActivityService],
      }),
      provideHttpClient(withInterceptors([includeBearerTokenInterceptor, sessioneScadutaInterceptor])),
      { provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, useValue: [soloApi] },
    ],
  };
}