import { Component, inject, signal, effect } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';
import { Tema, TemaService } from './tema/tema.service';

@Component({
  imports: [RouterOutlet, RouterLinkActive, RouterLink],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
  private readonly keycloak = inject(Keycloak);
  protected readonly nomeUtente = this.keycloak.tokenParsed?.['preferred_username'];
  readonly admin = this.keycloak.hasRealmRole('admin');
  private readonly eventiKeycloak = inject(KEYCLOAK_EVENT_SIGNAL);
  protected readonly temi = inject(TemaService);
  protected readonly scelte: { valore: Tema; etichetta: string }[] = [
    { valore: 'chiaro', etichetta: 'Tema chiaro' },
    { valore: 'scuro', etichetta: 'Tema scuro' },
    { valore: 'sistema', etichetta: 'Tema del sistema' },
  ];

  protected esci() {
    this.keycloak.logout({ redirectUri: window.location.origin });
  }

  protected cambiaPassword() {
    this.keycloak.login({ action: 'UPDATE_PASSWORD' });
  }

  constructor() {
    effect(() => {
      const evento = this.eventiKeycloak();
      if (evento.type === KeycloakEventType.AuthRefreshError) {
        this.keycloak.login();     // sessione finita: si rientra
      }
    });
  }
  /*protected caricaRiepilogo() {
      this.api.riepilogo().subscribe(risposta => this.riepilogo.set(risposta));
  }*/


}
