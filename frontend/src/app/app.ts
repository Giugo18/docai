import { JsonPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';

@Component({
  imports: [RouterOutlet, JsonPipe],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
  private readonly keycloak = inject(Keycloak);
  protected readonly nomeUtente = this.keycloak.tokenParsed?.['preferred_username'];
  private readonly http = inject(HttpClient);
  protected readonly riepilogo = signal<unknown>(null);

  protected esci() {
    this.keycloak.logout({ redirectUri: window.location.origin });
  }

  protected caricaRiepilogo() {
    this.http.get('/api/documenti/riepilogo')
      .subscribe(risposta => this.riepilogo.set(risposta));
  }
}
