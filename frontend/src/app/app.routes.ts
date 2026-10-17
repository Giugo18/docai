import { Routes } from '@angular/router';
import { PaginaDocumenti } from './documenti/pagina-documenti/pagina-documenti';
import { DettaglioDocumento } from './documenti/dettaglio-documento/dettaglio-documento';
import { soloAdmin } from './admin/solo-admin';
import { PaginaAdmin } from './admin/pagina-admin/pagina-admin';
import { PaginaChat } from './chat/pagina-chat/pagina-chat';
import { Conversazioni } from './rag/conversazioni/conversazioni';
import { soloAutenticati } from './auth/solo-autenticati';
import { PaginaPrivacy } from './legale/pagina-privacy/pagina-privacy';
import { PaginaTermini } from './legale/pagina-termini/pagina-termini';

export const routes: Routes = [
  // Pubbliche: leggibili anche senza account (l'informativa va letta PRIMA di entrare)
  { path: 'privacy', component: PaginaPrivacy },
  { path: 'termini', component: PaginaTermini },

  // Tutto il resto: solo con login
  {
    path: '',
    canActivateChild: [soloAutenticati],
    children: [
      { path: '', redirectTo: 'documenti', pathMatch: 'full' },
      { path: 'documenti', component: PaginaDocumenti },
      { path: 'documenti/:id', component: DettaglioDocumento },
      { path: 'admin', component: PaginaAdmin, canActivate: [soloAdmin] },
      { path: 'chat', component: PaginaChat },
      { path: 'chiedi', component: Conversazioni },
      { path: '**', redirectTo: 'documenti' },
    ],
  },
];