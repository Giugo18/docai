import { Fonte, Proposta } from './rag.model';

export type EsitoProposta = 'in attesa' | 'in corso' | 'confermata' | 'annullata' | 'errore';
export type PropostaVista = Proposta & { esito: EsitoProposta };

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
  proposte: PropostaVista[];
}