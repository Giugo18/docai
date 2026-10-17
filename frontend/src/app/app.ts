import { Component, ElementRef, effect, inject, signal, viewChild } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType } from 'keycloak-angular';
import { TemaService } from './tema/tema.service';

@Component({
  imports: [RouterOutlet, RouterLinkActive, RouterLink],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  host: {
    '(document:click)': 'chiudiSeFuori($event)',     // clic fuori dal menu → si chiude
    '(document:keydown.escape)': 'chiudiMenu()',      // Esc → si chiude
  },
})
export class App {
  protected readonly title = signal('frontend');
  private readonly keycloak = inject(Keycloak);
  /** Con check-sso si può essere sull'app anche senza login (pagine /privacy e /termini) */
  protected readonly autenticato = !!this.keycloak.authenticated;
  protected readonly nomeUtente: string = this.keycloak.tokenParsed?.['preferred_username'] ?? '';
  protected readonly email: string = this.keycloak.tokenParsed?.['email'] ?? '';
  protected readonly iniziale = this.nomeUtente.charAt(0).toUpperCase();
  readonly admin = this.autenticato && this.keycloak.hasRealmRole('admin');
  private readonly eventiKeycloak = inject(KEYCLOAK_EVENT_SIGNAL);
  protected readonly temi = inject(TemaService);

  protected readonly menuAperto = signal(false);
  private readonly menuUtente = viewChild<ElementRef<HTMLElement>>('menuUtente');

  protected apriChiudiMenu() {
    this.menuAperto.update(aperto => !aperto);
  }

  protected chiudiMenu() {
    this.menuAperto.set(false);
  }

  protected chiudiSeFuori(evento: MouseEvent) {
    const menu = this.menuUtente()?.nativeElement;
    if (menu && !menu.contains(evento.target as Node)) {
      this.chiudiMenu();
    }
  }

  protected entra() {
    this.keycloak.login({ redirectUri: window.location.origin + '/documenti' });
  }

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
}