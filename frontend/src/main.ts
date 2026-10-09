import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { ConfigApp, creaAppConfig } from './app/app.config';

// Prima la configurazione dell'ambiente, poi l'avvio di Angular:
// provideKeycloak ha bisogno dell'URL già quando vengono creati i providers
fetch('/config.json')
  .then((risposta) => {
    if (!risposta.ok) {
      throw new Error(`config.json non disponibile: HTTP ${risposta.status}`);
    }
    return risposta.json() as Promise<ConfigApp>;
  })
  .then((config) => bootstrapApplication(App, creaAppConfig(config)))
  .catch((errore) => console.error(errore));