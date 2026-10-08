import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { DocumentoDto, RiepilogoDocumenti } from './documento';

@Service()
export class DocumentiApi {
    private readonly http = inject(HttpClient);

  elenca() {
    return this.http.get<DocumentoDto[]>('/api/documenti');
  }

  riepilogo() {
    return this.http.get<RiepilogoDocumenti>('/api/documenti/riepilogo');
  }
}
