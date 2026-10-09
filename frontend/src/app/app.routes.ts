import { Routes } from '@angular/router';
import { PaginaDocumenti } from './documenti/pagina-documenti/pagina-documenti';
import { DettaglioDocumento } from './documenti/dettaglio-documento/dettaglio-documento';
import { soloAdmin } from './admin/solo-admin';
import { PaginaAdmin } from './admin/pagina-admin/pagina-admin';

export const routes: Routes = [
  { path: '', redirectTo: 'documenti', pathMatch: 'full' },
  { path: 'documenti', component: PaginaDocumenti },
  { path: 'documenti/:id', component: DettaglioDocumento },
  { path: 'admin', component: PaginaAdmin, canActivate: [soloAdmin] },
   { path: '**', redirectTo: 'documenti' },
];