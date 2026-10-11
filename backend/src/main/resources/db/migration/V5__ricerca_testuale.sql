-- Colonna calcolata da Postgres: il testo del chunk ridotto a radici di parole italiane.
-- Spring AI non la conosce e non la scrive: la mantiene il DB a ogni INSERT/UPDATE.
ALTER TABLE vector_store
    ADD COLUMN contenuto_tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('italian', content)) STORED;

-- Indice per la ricerca per parole (lo stesso tipo che usano i motori di ricerca)
CREATE INDEX idx_vector_store_tsv ON vector_store USING gin (contenuto_tsv);