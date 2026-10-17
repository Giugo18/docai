import { Injectable, computed, effect, signal } from '@angular/core';

/** La scelta esplicita dell'utente. null = non ha mai cliccato: si segue il sistema operativo */
type Scelta = 'chiaro' | 'scuro';
const CHIAVE = 'leggio18.tema';

@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly media = window.matchMedia('(prefers-color-scheme: dark)');
  private readonly sistemaScuro = signal(this.media.matches);
  private readonly scelta = signal<Scelta | null>(this.leggi());

  /** true = tema scuro attivo (scelta dell'utente, oppure il sistema se non ha mai scelto) */
  readonly scuro = computed(() => (this.scelta() ?? (this.sistemaScuro() ? 'scuro' : 'chiaro')) === 'scuro');

  constructor() {
    // ogni volta che cambia: applico la classe "dark" alla pagina
    effect(() => {
      const scuro = this.scuro();
      document.documentElement.classList.toggle('dark', scuro);
      document.documentElement.style.colorScheme = scuro ? 'dark' : 'light'; // scrollbar e campi nativi
    });
    // finché l'utente non sceglie, seguo il sistema anche a pagina aperta
    this.media.addEventListener('change', e => this.sistemaScuro.set(e.matches));
  }

  /** L'interruttore: acceso ↔ spento. Da qui in poi vale la scelta dell'utente */
  alterna(): void {
    const nuova: Scelta = this.scuro() ? 'chiaro' : 'scuro';
    this.scelta.set(nuova);
    try { localStorage.setItem(CHIAVE, nuova); } catch { /* storage non disponibile */ }
  }

  private leggi(): Scelta | null {
    try {
      const salvato = localStorage.getItem(CHIAVE);
      if (salvato === 'chiaro' || salvato === 'scuro') return salvato;
    } catch { /* storage non disponibile */ }
    return null;   // mai scelto, oppure il vecchio valore "sistema"
  }
}