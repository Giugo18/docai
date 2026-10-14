-- Nome leggibile dell'utente (preferred_username di Keycloak), accanto al sub.
-- Nullable: le righe già registrate non ce l'hanno.
ALTER TABLE consumo ADD COLUMN utente VARCHAR(100);