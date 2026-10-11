import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import Keycloak from 'keycloak-js';
import { catchError, throwError } from 'rxjs';

/** Un 401 dal backend vuol dire "token non più valido": si torna al login. */
export const sessioneScadutaInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloak = inject(Keycloak);
  return next(req).pipe(
    catchError((errore: HttpErrorResponse) => {
      if (errore.status === 401) {
        keycloak.login();
      }
      return throwError(() => errore);
    }),
  );
};