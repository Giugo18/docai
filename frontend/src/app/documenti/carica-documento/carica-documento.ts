import { Component, inject, signal } from '@angular/core';
import { DocumentiApi } from '../documenti-api';

@Component({
  imports: [],
  selector: 'app-carica-documento',
  styleUrl: './carica-documento.scss',
  templateUrl: './carica-documento.html',
})
export class CaricaDocumento {
  private readonly api = inject(DocumentiApi);
  protected readonly file = signal<File | null>(null);
  protected readonly inCaricamento = signal(false);
  protected readonly messaggio = signal('');

  protected scegli(evento: Event) {
    const input = evento.target as HTMLInputElement;
    this.file.set(input.files?.[0] ?? null);
    this.messaggio.set('');
  }

  protected invia() {
    const file = this.file();
    if (!file) {
      return;
    }
    this.inCaricamento.set(true);
    this.api.carica(file).subscribe({
      next: documento => {
        this.messaggio.set(`Caricato: ${documento.nomeFile}`);
        this.inCaricamento.set(false);
        this.api.aggiorna();          // la tabella si aggiorna da sola
      },
      error: () => {
        this.messaggio.set('Caricamento non riuscito');
        this.inCaricamento.set(false);
      },
    });
  }
}
