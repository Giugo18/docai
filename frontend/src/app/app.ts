import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';

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

  protected esci() {
    this.keycloak.logout({ redirectUri: window.location.origin });
  }

  /*protected caricaRiepilogo() {
      this.api.riepilogo().subscribe(risposta => this.riepilogo.set(risposta));
  }*/
}
