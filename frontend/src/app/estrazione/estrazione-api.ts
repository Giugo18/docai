import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { EsitoEstrazione } from './estrazione.model';

@Service()
export class EstrazioneApi {
  private readonly http = inject(HttpClient);

  /** Chiede al backend di estrarre la fattura dal PDF già caricato. */
  estraiFattura(idDocumento: string) {
    return this.http.post<EsitoEstrazione>(
      `/api/estrazione/documenti/${idDocumento}/fattura`,
      null,          // nessun body: il documento è già sul server, basta l'id
    );
  }
}