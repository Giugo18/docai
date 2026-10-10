import { Component, inject, input, signal } from '@angular/core';
import { EstrazioneApi } from '../estrazione-api';
import { EsitoEstrazione } from '../estrazione.model';
import { messaggioErrore } from '../../errori/problem-detail';
import { DatePipe, DecimalPipe } from '@angular/common';

@Component({
  imports: [DatePipe, DecimalPipe],
  selector: 'app-estrazione-fattura',
  styleUrl: './estrazione-fattura.scss',
  templateUrl: './estrazione-fattura.html',
})
export class EstrazioneFattura {

  private readonly api = inject(EstrazioneApi);

  readonly idDocumento = input.required<string>();   // passato dal padre

  protected readonly inCorso = signal(false);
  protected readonly esito = signal<EsitoEstrazione | null>(null);
  protected readonly errore = signal('');

  estrai() {
    this.inCorso.set(true);
    this.errore.set('');
    this.esito.set(null);

    this.api.estraiFattura(this.idDocumento()).subscribe({
      next: e => {
        this.esito.set(e);
        this.inCorso.set(false);
      },
      error: err => {
        this.errore.set(messaggioErrore(err, 'Estrazione non riuscita'));
        this.inCorso.set(false);
      },
    });
  }
}
