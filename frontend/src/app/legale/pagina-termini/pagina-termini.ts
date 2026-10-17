import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-pagina-termini',
  imports: [RouterLink],
  templateUrl: './pagina-termini.html',
})
export class PaginaTermini {
  readonly contatto = 'dunkel4ever@gmail.com';
}