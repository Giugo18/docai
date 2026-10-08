import { Component, inject } from '@angular/core';
import { DocumentiApi } from '../documenti-api';
import { DecimalPipe, KeyValuePipe } from '@angular/common';

@Component({
  imports: [KeyValuePipe, DecimalPipe],
  selector: 'app-riepilogo',
  styleUrl: './riepilogo.scss',
  templateUrl: './riepilogo.html',
})
export class Riepilogo {
  protected readonly riepilogo = inject(DocumentiApi).riepilogo;
}
