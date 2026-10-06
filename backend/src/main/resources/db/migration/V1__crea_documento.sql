-- Metadati dei documenti caricati.
-- I chunk vettoriali (settimana 8) finiranno nella tabella vector_store creata da Spring AI.
CREATE TABLE documento (
    id            UUID         PRIMARY KEY,
    nome_file     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    dimensione    BIGINT       NOT NULL,
    proprietario  VARCHAR(100) NOT NULL,   -- "sub" del JWT di Keycloak
    stato         VARCHAR(20)  NOT NULL,   -- CARICATO, INDICIZZATO, ERRORE
    caricato_il   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_documento_proprietario ON documento (proprietario);
