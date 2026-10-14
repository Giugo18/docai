import { Injectable, effect, signal } from '@angular/core';

export type Tema = 'chiaro' | 'scuro' | 'sistema';
const CHIAVE = 'leggio18.tema';

@Injectable({ providedIn: 'root' })
export class TemaService {
  private readonly sistemaScuro = window.matchMedia('(prefers-color-scheme: dark)');
  readonly tema = signal<Tema>(this.leggi());

  constructor() {
    // ogni volta che cambia la scelta: la salvo e la applico
    effect(() => {
      const scelta = this.tema();
      try { localStorage.setItem(CHIAVE, scelta); } catch { /* storage non disponibile */ }
      this.applica();
    });
    // con "sistema", segue il cambio di tema del sistema operativo anche a pagina aperta
    this.sistemaScuro.addEventListener('change', () => this.applica());
  }

  imposta(scelta: Tema): void {
    this.tema.set(scelta);
  }

  private applica(): void {
    const scuro = this.tema() === 'scuro' || (this.tema() === 'sistema' && this.sistemaScuro.matches);
    document.documentElement.classList.toggle('dark', scuro);
    document.documentElement.style.colorScheme = scuro ? 'dark' : 'light'; // scrollbar e campi nativi
  }

  private leggi(): Tema {
    try {
      const salvato = localStorage.getItem(CHIAVE);
      if (salvato === 'chiaro' || salvato === 'scuro' || salvato === 'sistema') return salvato;
    } catch { /* storage non disponibile */ }
    return 'sistema';
  }
}