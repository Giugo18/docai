import { HttpClient, HttpDownloadProgressEvent, HttpEvent, HttpEventType } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { filter, map, Observable } from 'rxjs';
import { EventoRag, StatoRisposta } from './rag.model';

@Service()
export class RagApi {
  private readonly http = inject(HttpClient);

  chiediInStreaming(domanda: string, documentoId: string | null): Observable<StatoRisposta> {
    return comeStato(this.http.post('/api/rag/domanda/stream', { domanda, documentoId }, {
      responseType: 'text',
      observe: 'events',
      reportProgress: true,
    }));
  }
}

/** Trasforma gli eventi HTTP di uno stream SSE nello stato completo della risposta. */
export function comeStato(eventi: Observable<HttpEvent<string>>): Observable<StatoRisposta> {
  return eventi.pipe(
    filter(e => e.type === HttpEventType.DownloadProgress || e.type === HttpEventType.Response),
    map(e => statoDa(e.type === HttpEventType.Response
        ? (e.body ?? '')
        : ((e as HttpDownloadProgressEvent).partialText ?? ''))),
  );
}

/** Rilegge tutto il testo arrivato finora e ricostruisce fonti, risposta e domanda riscritta. */
function statoDa(grezzo: string): StatoRisposta {
  const stato: StatoRisposta = { fonti: [], testo: '', riscritta: null };
  for (const riga of grezzo.split('\n')) {
    if (!riga.startsWith('data:')) continue;
    try {
      const evento = JSON.parse(riga.slice(5)) as EventoRag;
      if (evento.tipo === 'fonti') {
        stato.fonti = evento.fonti ?? [];
      } else if (evento.tipo === 'testo') {
        stato.testo += evento.testo ?? '';
      } else if (evento.tipo === 'riscritta') {
        stato.riscritta = evento.testo;
      }
      // altri tipi: ignorati
    } catch {
      // l'ultima riga può essere ancora a metà: arriverà intera al prossimo evento
    }
  }
  return stato;
}