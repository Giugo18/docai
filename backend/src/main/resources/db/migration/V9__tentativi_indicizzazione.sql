-- Quante volte l'indicizzazione è fallita di fila: il batch smette di riprovare oltre il massimo.
ALTER TABLE documento ADD COLUMN tentativi_indicizzazione INTEGER NOT NULL DEFAULT 0;