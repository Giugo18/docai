import { Component, inject, signal } from '@angular/core';
import { ChatApi } from '../chat-api';
import { messaggioErrore } from '../../errori/problem-detail';

@Component({
  selector: 'app-pagina-chat',
  templateUrl: './pagina-chat.html',
})
export class PaginaChat {
  private readonly api = inject(ChatApi);

  readonly domanda = signal('');
  readonly risposta = signal('');
  readonly inCorso = signal(false);
  readonly errore = signal<string | null>(null);

  scrivi(evento: Event) {
    this.domanda.set((evento.target as HTMLTextAreaElement).value);
  }

  invia() {
    const testo = this.domanda().trim();
    if (!testo || this.inCorso()) return;

    this.risposta.set('');
    this.errore.set(null);
    this.inCorso.set(true);

    this.api.chiediInStreaming(testo).subscribe({
      next: (parziale) => this.risposta.set(parziale),
      error: (e) => {
        this.errore.set(messaggioErrore(e, 'La risposta non è arrivata. Riprova.'));
        this.inCorso.set(false);
      },
      complete: () => this.inCorso.set(false),
    });
  }
}