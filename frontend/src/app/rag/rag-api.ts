import { HttpClient, HttpDownloadProgressEvent, HttpEventType } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { filter, map, Observable } from 'rxjs';
import { EventoRag, StatoRisposta } from './rag.model';

@Service()
export class RagApi {
  private readonly http = inject(HttpClient);

  chiediInStreaming(domanda: string, documentoId: string | null): Observable<StatoRisposta> {
    return this.http.post('/api/rag/domanda/stream', { domanda, documentoId }, {
      responseType: 'text',
      observe: 'events',
      reportProgress: true,
    }).pipe(
      filter(e => e.type === HttpEventType.DownloadProgress || e.type === HttpEventType.Response),
      map(e => statoDa(e.type === HttpEventType.Response
          ? (e.body ?? '')
          : ((e as HttpDownloadProgressEvent).partialText ?? ''))),
    );
  }
}

/** Rilegge tutto il testo arrivato finora e ricostruisce fonti + risposta. */
function statoDa(grezzo: string): StatoRisposta {
  const stato: StatoRisposta = { fonti: [], testo: '' };
  for (const riga of grezzo.split('\n')) {
    if (!riga.startsWith('data:')) continue;
    try {
      const evento = JSON.parse(riga.slice(5)) as EventoRag;
      if (evento.tipo === 'fonti') {
        stato.fonti = evento.fonti ?? [];
      } else {
        stato.testo += evento.testo ?? '';
      }
    } catch {
      // l'ultima riga può essere ancora a metà: arriverà intera al prossimo evento
    }
  }
  return stato;
}