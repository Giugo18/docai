export interface DocumentoDto {
  id: string;
  nomeFile: string;
  dimensione: number;
  stato: 'CARICATO' | 'INDICIZZATO' | 'ERRORE';
  caricatoIl: string;   // data in formato ISO, ad esempio "2026-10-06T07:48:50Z"
}

export interface RiepilogoDocumenti {
  totale: number;
  dimensioneTotale: number;
  perStato: Record<string, number>;
}