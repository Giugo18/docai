import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
  private readonly keycloak = inject(Keycloak);
  protected readonly nomeUtente = this.keycloak.tokenParsed?.['preferred_username'];
  protected esci() {
    this.keycloak.logout({ redirectUri: window.location.origin });
  }
}
