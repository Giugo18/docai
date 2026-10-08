import { Component, inject } from '@angular/core';
import { DocumentiApi } from '../documenti-api';
import { Riepilogo } from '../riepilogo/riepilogo';
import { CaricaDocumento } from '../carica-documento/carica-documento';
import { ElencoDocumenti } from '../elenco-documenti/elenco-documenti';

@Component({
  imports: [Riepilogo, CaricaDocumento, ElencoDocumenti],
  selector: 'app-pagina-documenti',
  styleUrl: './pagina-documenti.scss',
  templateUrl: './pagina-documenti.html',
})
export class PaginaDocumenti {

  constructor() {
    inject(DocumentiApi).aggiorna();   // carica lista e riepilogo all'apertura della pagina
  }
}
