import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { DocumentiApi } from '../documenti-api';

@Component({
  imports: [[DatePipe, DecimalPipe]],
  selector: 'app-elenco-documenti',
  styleUrl: './elenco-documenti.scss',
  templateUrl: './elenco-documenti.html',
})
export class ElencoDocumenti {
   private readonly api = inject(DocumentiApi);

  protected readonly documenti = this.api.documenti;;

  constructor() {
    this.api.aggiorna();   // carica la lista quando il componente nasce
  }
}
