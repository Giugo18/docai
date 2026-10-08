import { HttpErrorResponse } from "@angular/common/http";

// Il formato degli errori del backend (RFC 9457), lo stesso del ProblemDetail di Spring
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
}

// Restituisce il messaggio del backend, oppure un testo di riserva
export function messaggioErrore(errore: unknown, predefinito: string): string {
  if (errore instanceof HttpErrorResponse) {
    const problema = errore.error as ProblemDetail | null;
    if (problema?.detail) {
      return problema.detail;
    }
  }
  return predefinito;
}