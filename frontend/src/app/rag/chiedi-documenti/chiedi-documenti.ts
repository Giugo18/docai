import { Component, inject, input, OnDestroy, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { RagApi } from '../rag-api';
import { Fonte } from '../rag.model';
import { messaggioErrore } from '../../errori/problem-detail';

@Component({
  selector: 'app-chiedi-documenti',
  imports: [DecimalPipe, RouterLink],
  templateUrl: './chiedi-documenti.html',
  styleUrl: './chiedi-documenti.scss',
})
export class ChiediDocumenti implements OnDestroy {
  private readonly api = inject(RagApi);

  /** null = tutti i miei documenti; un id = solo quel documento. */
  readonly documentoId = input<string | null>(null);

  protected readonly domanda = signal('');
  protected readonly risposta = signal('');
  protected readonly fonti = signal<Fonte[]>([]);
  protected readonly inCorso = signal(false);
  protected readonly errore = signal('');
  private richiesta?: Subscription;

  scrivi(evento: Event) {
    this.domanda.set((evento.target as HTMLTextAreaElement).value);
  }

  invia() {
    const testo = this.domanda().trim();
    if (!testo || this.inCorso()) return;

    this.richiesta?.unsubscribe();
    this.risposta.set('');
    this.fonti.set([]);
    this.errore.set('');
    this.inCorso.set(true);

    this.richiesta = this.api.chiediInStreaming(testo, this.documentoId()).subscribe({
      next: stato => {
        this.fonti.set(stato.fonti);
        this.risposta.set(stato.testo);
      },
      error: e => {
        this.errore.set(messaggioErrore(e, 'Domanda non riuscita'));
        this.inCorso.set(false);
      },
      complete: () => this.inCorso.set(false),
    });
  }

  ngOnDestroy() {
    this.richiesta?.unsubscribe();   // lasci la pagina: la richiesta HTTP viene annullata
  }
}