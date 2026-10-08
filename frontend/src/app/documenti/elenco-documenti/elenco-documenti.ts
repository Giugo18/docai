import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { DocumentiApi } from '../documenti-api';
import { RouterLink } from '@angular/router';
import { COLORI_STATO, DocumentoDto } from '../documento';

@Component({
  imports: [DatePipe, DecimalPipe, RouterLink],
  selector: 'app-elenco-documenti',
  styleUrl: './elenco-documenti.scss',
  templateUrl: './elenco-documenti.html',
})
export class ElencoDocumenti {

   
private readonly api = inject(DocumentiApi);

protected readonly documenti = this.api.documenti;
readonly coloriStato = COLORI_STATO   // import { COLORI_STATO } from '../documento';

}
