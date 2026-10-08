import { HttpClient } from '@angular/common/http';
import { inject, Service, signal } from '@angular/core';
import { DocumentoDto, RiepilogoDocumenti } from './documento';

@Service()
export class DocumentiApi {
    private readonly http = inject(HttpClient);
    private readonly _documenti = signal<DocumentoDto[]>([]);
    readonly documenti = this._documenti.asReadonly();   // gli altri leggono, solo il servizio scrive
    private readonly _riepilogo = signal<RiepilogoDocumenti | null>(null);
    readonly riepilogo = this._riepilogo.asReadonly();


  elenca() {
    return this.http.get<DocumentoDto[]>('/api/documenti');
  }

  aggiorna() {
    this.http.get<DocumentoDto[]>('/api/documenti')
      .subscribe(lista => this._documenti.set(lista));
    this.http.get<RiepilogoDocumenti>('/api/documenti/riepilogo')
      .subscribe(r => this._riepilogo.set(r));
  }

  carica(file: File) {
    const dati = new FormData();
    dati.append('file', file);   // "file" = il nome del @RequestParam("file") nel controller Java
    return this.http.post<DocumentoDto>('/api/documenti', dati);
  }

  trova(id: string) {
    return this.http.get<DocumentoDto>(`/api/documenti/${id}`);
  }
}
