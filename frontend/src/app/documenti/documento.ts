export interface DocumentoDto {
  id: string;
  nomeFile: string;
  dimensione: number;
  stato: 'CARICATO' | 'INDICIZZATO' | 'ERRORE' | 'IN_INDICIZZAZIONE';
  caricatoIl: string;   // data in formato ISO, ad esempio "2026-10-06T07:48:50Z"
}

export interface RiepilogoDocumenti {
  totale: number;
  dimensioneTotale: number;
  perStato: Record<string, number>;
}

export const COLORI_STATO: Record<DocumentoDto['stato'], string> = {
  CARICATO:          'bg-blue-50 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300',
  INDICIZZATO:       'bg-green-50 text-green-700 dark:bg-green-900/40 dark:text-green-300',
  ERRORE:            'bg-red-50 text-red-700 dark:bg-red-900/40 dark:text-red-300',
  IN_INDICIZZAZIONE: 'bg-blue-50 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300',
};