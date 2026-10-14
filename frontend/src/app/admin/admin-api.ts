import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';

export interface RigaStatistica {
  proprietario: string;
  documenti: number;
  dimensione: number;
}

export interface ConsumoUtente {
  proprietario: string;
  utente: string | null;
  operazione: string;
  modello: string;
  chiamate: number;
  tokenIn: number;
  tokenOut: number;
  costoStimato: number;
}

export interface RiepilogoConsumi {
  mese: string;          // "2026-10"
  valuta: string;        // "USD"
  righe: ConsumoUtente[];
  totale: number;
}

@Service()
export class AdminApi {
  private readonly http = inject(HttpClient);

  statistiche() {
    return this.http.get<RigaStatistica[]>('/api/admin/statistiche');
  }

  consumi(mese: string) {
    return this.http.get<RiepilogoConsumi>('/api/admin/consumi', { params: { mese } });
  }
}