-- Vettori di modelli diversi non sono confrontabili: si svuota e si ricalcola.
TRUNCATE vector_store;

ALTER TABLE vector_store ALTER COLUMN embedding TYPE vector(1024);