import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { DocumentiApi } from '../documenti-api';
import { RouterLink } from '@angular/router';

@Component({
  imports: [DatePipe, DecimalPipe, RouterLink],
  selector: 'app-elenco-documenti',
  styleUrl: './elenco-documenti.scss',
  templateUrl: './elenco-documenti.html',
})
export class ElencoDocumenti {
   private readonly api = inject(DocumentiApi);

  protected readonly documenti = this.api.documenti;;

}
