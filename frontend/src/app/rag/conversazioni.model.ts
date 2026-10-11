import { Fonte } from './rag.model';

export interface ConversazioneDto {
  id: string;
  titolo: string;
  documentoId: string | null;
  aggiornataIl: string;
}

export interface MessaggioDto {
  ruolo: 'UTENTE' | 'ASSISTENTE';
  testo: string;
  fonti: Fonte[];
  creatoIl: string;
}

export interface DettaglioConversazione {
  conversazione: ConversazioneDto;
  messaggi: MessaggioDto[];
}

/** Un messaggio come lo mostra la pagina, compreso quello che si sta ancora scrivendo. */
export interface MessaggioVista {
  ruolo: 'UTENTE' | 'ASSISTENTE';
  testo: string;
  fonti: Fonte[];
  riscritta: string | null;
  inCorso: boolean;
}