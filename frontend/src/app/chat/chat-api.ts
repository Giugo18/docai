import { inject, Service } from '@angular/core';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { Observable, filter, map } from 'rxjs';

// usa lo stesso decoratore e gli stessi import di documenti-api.ts
@Service()
export class ChatApi {
  private readonly http = inject(HttpClient);

  /** Emette il testo della risposta accumulato finora, a ogni pezzo che arriva */
  chiediInStreaming(domanda: string): Observable<string> {
    return this.http
      .post('/api/chat/stream', { domanda }, {
        responseType: 'text',
        observe: 'events',
        reportProgress: true,
      })
      .pipe(
        map((evento) =>
          evento.type === HttpEventType.DownloadProgress ? (evento.partialText ?? '')
          : evento.type === HttpEventType.Response ? (evento.body ?? '')
          : null),
        filter((testo): testo is string => testo !== null),
        map(estraiTesto),
      );
  }
}

/** Dal testo SSE grezzo (righe "data:{...}") ricava la risposta leggibile */
function estraiTesto(sse: string): string {
  let risposta = '';
  for (const riga of sse.split('\n')) {
    if (!riga.startsWith('data:')) continue;
    try {
      risposta += JSON.parse(riga.slice(5)).testo;
    } catch {
      // ultima riga ancora incompleta: sarà completa al prossimo giro
    }
  }
  return risposta;
}