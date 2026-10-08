import { HttpClient } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { DocumentoDto, RiepilogoDocumenti } from './documento';

@Service()
export class DocumentiApi {
    private readonly http = inject(HttpClient);
    private readonly _documenti = signal<DocumentoDto[]>([]);
    readonly documenti = this._documenti.asReadonly();   // gli altri leggono, solo il servizio scrive


  elenca() {
    return this.http.get<DocumentoDto[]>('/api/documenti');
  }

  riepilogo() {
    return this.http.get<RiepilogoDocumenti>('/api/documenti/riepilogo');
  }

  aggiorna() {
    this.http.get<DocumentoDto[]>('/api/documenti')
      .subscribe(lista => this._documenti.set(lista));
  }

  carica(file: File) {
    const dati = new FormData();
    dati.append('file', file);   // "file" = il nome del @RequestParam("file") nel controller Java
    return this.http.post<DocumentoDto>('/api/documenti', dati);
  }
}
