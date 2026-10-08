import { Component, inject, input, signal } from '@angular/core';
import { DocumentiApi } from '../documenti-api';
import { DocumentoDto } from '../documento';
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

  ngOnInit() {
    this.api.trova(this.id()).subscribe({
      next: d => this.documento.set(d),
      error: e => this.errore.set(messaggioErrore(e, 'Impossibile caricare il documento')),
    });
  }
}
