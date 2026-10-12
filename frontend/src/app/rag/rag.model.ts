export interface Fonte {
  numero: number;
  documentoId: string;
  nomeFile: string;
  pagina: number;
  punteggio: number;
  estratto: string;
}

export interface EventoRag {
  tipo: 'fonti' | 'testo' | 'riscritta' | 'proposta';
  fonti?: Fonte[];
  testo?: string;
  proposta?: Proposta;
}

/** Ciò che la pagina mostra: le fonti e il testo arrivato finora. */
export interface StatoRisposta {
  fonti: Fonte[];
  testo: string;
  riscritta: string | null;
  proposte: Proposta[];
}

export interface Proposta {
  tipo: 'RINOMINA';
  documentoId: string;
  nomeAttuale: string;
  nuovoNome: string;
}