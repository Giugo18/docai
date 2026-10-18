-- Optimistic locking: ogni modifica incrementa la versione.
-- Un salvataggio fatto su una copia vecchia (versione diversa) fallisce invece di sovrascrivere.
ALTER TABLE documento ADD COLUMN versione BIGINT NOT NULL DEFAULT 0;