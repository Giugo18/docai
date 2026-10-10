/** Specchio di RigaFattura.java */
export interface RigaFattura {
  descrizione: string | null;
  quantita: number | null;
  prezzoUnitario: number | null;
  importo: number | null;
}

/** Specchio di Fattura.java */
export interface Fattura {
  numero: string | null;
  data: string | null;              // LocalDate → "2026-03-15"
  fornitore: string | null;
  partitaIvaFornitore: string | null;
  cliente: string | null;
  righe: RigaFattura[] | null;
  imponibile: number | null;
  iva: number | null;
  totale: number | null;
}

/** Specchio di EsitoEstrazione.java */
export interface EsitoEstrazione {
  fattura: Fattura;
  avvisi: string[];
  tentativi: number;
}