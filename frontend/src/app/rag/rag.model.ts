export interface Fonte {
  numero: number;
  documentoId: string;
  nomeFile: string;
  pagina: number;
  punteggio: number;
  estratto: string;
}

export interface EventoRag {
  tipo: 'fonti' | 'testo' | 'riscritta';
  testo: string | null;
  fonti: Fonte[] | null;
}

/** Ciò che la pagina mostra: le fonti e il testo arrivato finora. */
export interface StatoRisposta {
  fonti: Fonte[];
  testo: string;
  riscritta: string | null;
}