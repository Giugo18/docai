import { Component, inject, input, signal } from '@angular/core';
import { DocumentiApi } from '../documenti-api';
import { COLORI_STATO, DocumentoDto } from '../documento';
import { RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe } from '@angular/common';
import { messaggioErrore } from '../../errori/problem-detail';

@Component({
  imports: [RouterLink, DatePipe, DecimalPipe],
  selector: 'app-dettaglio-documento',
  styleUrl: './dettaglio-documento.scss',
  templateUrl: './dettaglio-documento.html',
})
export class DettaglioDocumento {
  private readonly api = inject(DocumentiApi);

  readonly id = input.required<string>();   // riempito dal router con il :id dell'URL

  protected readonly documento = signal<DocumentoDto | null>(null);
  protected readonly nonTrovato = signal(false);

  protected readonly errore = signal('');
  readonly coloriStato = COLORI_STATO;
  readonly inModifica = signal(false);
  readonly nuovoNome = signal('');
  readonly erroreNome = signal('');
  readonly salvataggio = signal(false);

  ngOnInit() {
    this.api.trova(this.id()).subscribe({
      next: d => this.documento.set(d),
      error: e => this.errore.set(messaggioErrore(e, 'Impossibile caricare il documento')),
    });
  }

  modifica() {
    this.nuovoNome.set(this.documento()!.nomeFile);
    this.erroreNome.set('');
    this.inModifica.set(true);
  }

  annulla() {
    this.inModifica.set(false);
  }

  scrivi(evento: Event) {
    this.nuovoNome.set((evento.target as HTMLInputElement).value);
  }

  salva() {
    this.salvataggio.set(true);
    this.api.rinomina(this.id(), this.nuovoNome()).subscribe({
      next: aggiornato => {
        this.documento.set(aggiornato);
        this.inModifica.set(false);
        this.salvataggio.set(false);
        this.api.aggiorna();   // la lista è già aggiornata quando torni indietro
      },
      error: errore => {
        this.erroreNome.set(messaggioErrore(errore, 'Rinomina non riuscita'));
        this.salvataggio.set(false);
      },
    });
  }
}
