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
const stato: StatoRisposta = { fonti: [], testo: '', riscritta: null, proposte: [] };
  for (const riga of grezzo.split('\n')) {
    if (!riga.startsWith('data:')) continue;
    try {
      const evento = JSON.parse(riga.slice(5)) as EventoRag;
      if (evento.tipo === 'fonti') {
        stato.fonti = evento.fonti ?? [];
      } else if (evento.tipo === 'testo') {
        stato.testo += evento.testo ?? '';
      } else if (evento.tipo === 'riscritta') {
        stato.riscritta = evento.testo ?? null;
      } else if (evento.tipo === 'proposta' && evento.proposta) {
        stato.proposte.push(evento.proposta);
      }
      // altri tipi: ignorati
    } catch {
      // l'ultima riga può essere ancora a metà: arriverà intera al prossimo evento
    }
  }
  stato.testo = normalizzaCitazioni(stato.testo); 
  return stato;
}

const CITAZIONE = /[\[【]+\s*Fonte\s*(\d+)\s*[\]】]+/g;

/** 【Fonte 2】, [[Fonte 2]], [Fonte 2] con spazi speciali → [Fonte 2] */
export function normalizzaCitazioni(testo: string): string {
  return testo.replace(CITAZIONE, '[Fonte $1]');
}