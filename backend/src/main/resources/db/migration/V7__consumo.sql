-- Token consumati per utente: la base per i costi.
-- Le metriche Micrometer (26a) stanno in memoria e si azzerano a ogni riavvio; questa tabella no.
CREATE TABLE consumo (
                         id            BIGSERIAL     PRIMARY KEY,
                         proprietario  VARCHAR(100)  NOT NULL,   -- "sub" del JWT, come in documento
                         operazione    VARCHAR(30)   NOT NULL,   -- ASSISTENTE, AGENTE, ESTRAZIONE, ...
                         modello       VARCHAR(100)  NOT NULL,
                         token_in      INTEGER       NOT NULL,
                         token_out     INTEGER       NOT NULL,
                         creato_il     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- la query tipica è "consumi di un utente in un periodo"
CREATE INDEX idx_consumo_proprietario_data ON consumo (proprietario, creato_il);