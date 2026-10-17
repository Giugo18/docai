import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pagina-privacy',
  imports: [RouterLink],
  templateUrl: './pagina-privacy.html',
})
export class PaginaPrivacy {
  readonly contatto = 'dunkel4ever@gmail.com';
  readonly aggiornamento = '17 ottobre 2026';
}