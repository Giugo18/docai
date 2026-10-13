import { Component, inject, input, OnDestroy, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { map, of, Subscription, switchMap, tap } from 'rxjs';
import { ConversazioniApi } from '../conversazioni-api';
import { ConversazioneDto, EsitoProposta, MessaggioVista, PropostaVista } from '../conversazioni.model';
import { messaggioErrore } from '../../errori/problem-detail';
import { MarkdownPipe } from '../markdown.pipe';
import { DocumentiApi } from '../../documenti/documenti-api';

@Component({
  selector: 'app-conversazioni',
  imports: [DatePipe, RouterLink, MarkdownPipe],
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
  private readonly documenti = inject(DocumentiApi);

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
        ruolo: m.ruolo, testo: m.testo, fonti: m.fonti, riscritta: null, inCorso: false, proposte: []
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
    { ruolo: 'UTENTE', testo, fonti: [], riscritta: null, inCorso: false, proposte: [] },
    { ruolo: 'ASSISTENTE', testo: '', fonti: [], riscritta: null, inCorso: true, proposte: [] },
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
                next: stato => this.aggiornaUltimo({
          testo: stato.testo, fonti: stato.fonti, riscritta: stato.riscritta,
          proposte: stato.proposte.map(p => ({ ...p, esito: 'in attesa' as EsitoProposta })),
        }),
                error: e => {
          this.errore.set(messaggioErrore(e, 'Domanda non riuscita'));
          this.messaggi.update(lista => {
            const ultimo = lista.at(-1);
            return ultimo && !ultimo.testo
              ? lista.slice(0, -1)                                     // risposta vuota: la tolgo
              : [...lista.slice(0, -1), { ...ultimo!, inCorso: false }]; // risposta a metà: la tengo
          });
          this.inCorso.set(false);
        },
        complete: () => {
          this.aggiornaUltimo({ inCorso: false });
          this.inCorso.set(false);
          this.caricaElenco();       // la conversazione sale in cima all'elenco
        },
      });
  }

    conferma(indice: number, p: PropostaVista) {
    this.segnaEsito(indice, p, 'in corso');
    this.documenti.rinomina(p.documentoId, p.nuovoNome).subscribe({
      next: () => {
        this.segnaEsito(indice, p, 'confermata');
        this.documenti.aggiorna();          // l'elenco Documenti mostra subito il nome nuovo
      },
      error: e => {
        this.errore.set(messaggioErrore(e, 'Rinomina non riuscita'));
        this.segnaEsito(indice, p, 'errore');
      },
    });
  }

  annulla(indice: number, p: PropostaVista) {
    this.segnaEsito(indice, p, 'annullata');
  }

  /** Cambia l'esito di una proposta senza mutare gli oggetti: nuove liste e nuovi oggetti. */
  private segnaEsito(indice: number, p: PropostaVista, esito: EsitoProposta) {
    this.messaggi.update(lista => lista.map((m, i) => i !== indice ? m : {
      ...m,
            proposte: m.proposte.map(x =>
        x.documentoId === p.documentoId && x.nuovoNome === p.nuovoNome ? { ...x, esito } : x),
    }));
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