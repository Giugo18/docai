import { Component } from '@angular/core';

/** Obbligo di trasparenza (AI Act, art. 50): l'utente sa che le risposte sono generate da un'AI */
@Component({
  selector: 'app-avviso-ai',
  template: `
    <p class="mt-2 text-center text-xs text-slate-500 dark:text-slate-400">
      Risposte generate da un'intelligenza artificiale: possono contenere errori. Verifica le fonti citate.
    </p>
  `,
})
export class AvvisoAi {}