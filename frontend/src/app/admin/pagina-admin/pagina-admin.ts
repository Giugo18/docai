import { Component, inject, signal } from '@angular/core';
import { AdminApi, RigaStatistica } from '../admin-api';
import { messaggioErrore } from '../../errori/problem-detail';
import { DecimalPipe } from '@angular/common';

@Component({
  imports: [DecimalPipe],
  selector: 'app-pagina-admin',
  styleUrl: './pagina-admin.scss',
  templateUrl: './pagina-admin.html',
})
export class PaginaAdmin {
  private readonly api = inject(AdminApi);

  readonly righe = signal<RigaStatistica[] | null>(null);
  readonly errore = signal('');

  ngOnInit() {
    this.api.statistiche().subscribe({
      next: righe => this.righe.set(righe),
      error: e => this.errore.set(messaggioErrore(e, 'Impossibile caricare le statistiche')),
    });
  }
}
