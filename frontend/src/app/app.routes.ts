import { Routes } from '@angular/router';
import { PaginaDocumenti } from './documenti/pagina-documenti/pagina-documenti';
import { DettaglioDocumento } from './documenti/dettaglio-documento/dettaglio-documento';

export const routes: Routes = [
  { path: '', redirectTo: 'documenti', pathMatch: 'full' },
  { path: 'documenti', component: PaginaDocumenti },
  { path: 'documenti/:id', component: DettaglioDocumento },
  { path: '**', redirectTo: 'documenti' },
];