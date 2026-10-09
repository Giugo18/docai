import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';

export interface RigaStatistica {
  proprietario: string;
  documenti: number;
  dimensione: number;
}

@Service()
export class AdminApi {
  private readonly http = inject(HttpClient);

  statistiche() {
    return this.http.get<RigaStatistica[]>('/api/admin/statistiche');
  }
}