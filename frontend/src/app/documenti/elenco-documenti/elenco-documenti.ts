import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { DocumentiApi } from '../documenti-api';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  imports: [[DatePipe, DecimalPipe]],
  selector: 'app-elenco-documenti',
  styleUrl: './elenco-documenti.scss',
  templateUrl: './elenco-documenti.html',
})
export class ElencoDocumenti {
   private readonly api = inject(DocumentiApi);

  protected readonly documenti = toSignal(this.api.elenca(), { initialValue: [] });
}
