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
    let corpo: unknown = errore.error;
    // con responseType 'text' il ProblemDetail arriva come stringa JSON
    if (typeof corpo === 'string') {
      try { corpo = JSON.parse(corpo); } catch { /* non era JSON: resta stringa */ }
    }
    const problema = corpo as ProblemDetail | null;
    if (problema?.detail) {
      return problema.detail;
    }
  }
  return predefinito;
}