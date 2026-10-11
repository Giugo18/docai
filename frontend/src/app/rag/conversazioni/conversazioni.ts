import { Component, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { map, of, Subscription, switchMap, tap } from 'rxjs';
import { ConversazioniApi } from '../conversazioni-api';
import { ConversazioneDto, MessaggioVista } from '../conversazioni.model';
import { messaggioErrore } from '../../errori/problem-detail';

@Component({
  selector: 'app-conversazioni',
  imports: [DatePipe, RouterLink],
  templateUrl: './conversazioni.html',
  styleUrl: './conversazioni.scss',
})
export class Conversazioni implements OnInit, OnDestroy {
  private readonly api = inject(ConversazioniApi);

  /** null = tutti i miei documenti; un id = solo le chat su quel documento. */
  readonly documentoId = input<string | null>(null);

  protected readonly conversazioni = signal<ConversazioneDto[]>([]);
  protected readonly attiva = signal<string | null>(null);
  protected readonly messaggi = signal<MessaggioVista[]>([]);
  protected readonly domanda = signal('');
  protected readonly inCorso = signal(false);
  protected readonly errore = signal('');
  private richiesta?: Subscription;

  ngOnInit() {
    this.caricaElenco();
  }

  ngOnDestroy() {
    this.richiesta?.unsubscribe();
  }

  nuova() {
    this.richiesta?.unsubscribe();
    this.attiva.set(null);
    this.messaggi.set([]);
    this.errore.set('');
    this.inCorso.set(false);
  }

  apri(id: string) {
    this.nuova();
    this.attiva.set(id);
    this.api.apri(id).subscribe({
      next: d => this.messaggi.set(d.messaggi.map(m => ({
        ruolo: m.ruolo, testo: m.testo, fonti: m.fonti, riscritta: null, inCorso: false,
      }))),
      error: e => this.errore.set(messaggioErrore(e, 'Conversazione non disponibile')),
    });
  }

  elimina(c: ConversazioneDto, evento: Event) {
    evento.stopPropagation();          // il clic su ✕ non deve anche aprire la conversazione
    if (!confirm(`Eliminare "${c.titolo}"?`)) return;
    this.api.elimina(c.id).subscribe({
      next: () => {
        if (this.attiva() === c.id) this.nuova();
        this.caricaElenco();
      },
      error: e => this.errore.set(messaggioErrore(e, 'Eliminazione non riuscita')),
    });
  }

  scrivi(evento: Event) {
    this.domanda.set((evento.target as HTMLTextAreaElement).value);
  }

  /** La frase da mostrare mentre la risposta non ha ancora testo. */
  protected fase(m: MessaggioVista, indice: number): string {
    if (m.fonti.length) {
      return `Leggo ${m.fonti.length} ${m.fonti.length === 1 ? 'fonte' : 'fonti'} e preparo la risposta`;
    }
    if (m.riscritta !== null) return 'Cerco nei documenti';
    return indice > 1 ? 'Riformulo la domanda' : 'Cerco nei documenti';   // > 1: c'è già uno scambio prima
  }

  invia() {
    const testo = this.domanda().trim();
    if (!testo || this.inCorso()) return;

    this.domanda.set('');
    this.errore.set('');
    this.inCorso.set(true);
    this.messaggi.update(lista => [...lista,
    { ruolo: 'UTENTE', testo, fonti: [], riscritta: null, inCorso: false },
    { ruolo: 'ASSISTENTE', testo: '', fonti: [], riscritta: null, inCorso: true },
    ]);

    // conversazione già aperta → la uso; altrimenti la creo, con la domanda come titolo
    const idConversazione$ = this.attiva()
      ? of(this.attiva()!)
      : this.api.crea(testo.slice(0, 80), this.documentoId()).pipe(
        tap(c => this.attiva.set(c.id)),
        map(c => c.id));

    this.richiesta = idConversazione$
      .pipe(switchMap(id => this.api.chiedi(id, testo)))
      .subscribe({
        next: stato => this.aggiornaUltimo({ testo: stato.testo, fonti: stato.fonti, riscritta: stato.riscritta }),
        error: e => {
          this.errore.set(messaggioErrore(e, 'Domanda non riuscita'));
          this.aggiornaUltimo({ inCorso: false });
          this.inCorso.set(false);
        },
        complete: () => {
          this.aggiornaUltimo({ inCorso: false });
          this.inCorso.set(false);
          this.caricaElenco();       // la conversazione sale in cima all'elenco
        },
      });
  }

  private caricaElenco() {
    this.api.elenca(this.documentoId()).subscribe({
      next: lista => this.conversazioni.set(lista),
      error: e => this.errore.set(messaggioErrore(e, 'Impossibile caricare le conversazioni')),
    });
  }

  /** Aggiorna il messaggio dell'assistente in fondo alla lista. */
  private aggiornaUltimo(modifiche: Partial<MessaggioVista>) {
    this.messaggi.update(lista => [...lista.slice(0, -1), { ...lista.at(-1)!, ...modifiche }]);
  }
}