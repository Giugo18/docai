import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { AdminApi, ConsumoUtente, RiepilogoConsumi, RigaStatistica } from '../admin-api';
import { messaggioErrore } from '../../errori/problem-detail';

@Component({
  imports: [DecimalPipe, CurrencyPipe],
  selector: 'app-pagina-admin',
  styleUrl: './pagina-admin.scss',
  templateUrl: './pagina-admin.html',
})
export class PaginaAdmin implements OnInit {
  private readonly api = inject(AdminApi);

  readonly righe = signal<RigaStatistica[] | null>(null);
  readonly errore = signal('');

  readonly mese = signal(meseCorrente());
  readonly consumi = signal<RiepilogoConsumi | null>(null);
  readonly erroreConsumi = signal('');

  /** Le righe raggruppate per utente: un gruppo = una cella "Utente" alta quanto le sue operazioni. */
  readonly gruppi = computed(() => {
    const riepilogo = this.consumi();
    if (!riepilogo) return [];
    const perUtente = new Map<string, ConsumoUtente[]>();
    for (const r of riepilogo.righe) {
      perUtente.set(r.proprietario, [...(perUtente.get(r.proprietario) ?? []), r]);
    }
    return [...perUtente.values()].map(righe => ({
      proprietario: righe[0].proprietario,
      utente: righe[0].utente ?? righe[0].proprietario.slice(0, 8) + '…',
      costo: righe.reduce((somma, r) => somma + r.costoStimato, 0),
      righe,
    }));
  });

  ngOnInit() {
    this.api.statistiche().subscribe({
      next: righe => this.righe.set(righe),
      error: e => this.errore.set(messaggioErrore(e, 'Impossibile caricare le statistiche')),
    });
    this.caricaConsumi();
  }

  cambiaMese(valore: string) {
    if (!valore) return;               // l'utente ha svuotato il campo
    this.mese.set(valore);
    this.caricaConsumi();
  }

  private caricaConsumi() {
    this.consumi.set(null);
    this.erroreConsumi.set('');
    this.api.consumi(this.mese()).subscribe({
      next: c => this.consumi.set(c),
      error: e => this.erroreConsumi.set(messaggioErrore(e, 'Impossibile caricare i consumi')),
    });
  }
}

/** "2026-10": il formato di <input type="month"> e di YearMonth in Java. */
function meseCorrente(): string {
  const oggi = new Date();
  return `${oggi.getFullYear()}-${String(oggi.getMonth() + 1).padStart(2, '0')}`;
}