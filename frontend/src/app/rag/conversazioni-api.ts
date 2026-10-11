import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { comeStato } from './rag-api';
import { ConversazioneDto, DettaglioConversazione } from './conversazioni.model';

@Service()
export class ConversazioniApi {
  private readonly http = inject(HttpClient);

  elenca(documentoId: string | null) {
    const params: Record<string, string> = documentoId ? { documentoId } : {};
    return this.http.get<ConversazioneDto[]>('/api/conversazioni', { params });
  }

  crea(titolo: string, documentoId: string | null) {
    return this.http.post<ConversazioneDto>('/api/conversazioni', { titolo, documentoId });
  }

  apri(id: string) {
    return this.http.get<DettaglioConversazione>(`/api/conversazioni/${id}`);
  }

  elimina(id: string) {
    return this.http.delete<void>(`/api/conversazioni/${id}`);
  }

  chiedi(id: string, domanda: string) {
    return comeStato(this.http.post(`/api/rag/conversazioni/${id}/domanda/stream`, { domanda },
        { responseType: 'text', observe: 'events', reportProgress: true }));
  }
}