CREATE TABLE conversazione (
                               id             UUID         PRIMARY KEY,
                               proprietario   VARCHAR(100) NOT NULL,                                  -- "sub" del JWT
                               titolo         VARCHAR(200) NOT NULL,
                               documento_id   UUID         REFERENCES documento (id) ON DELETE CASCADE, -- null = tutti i documenti
                               creata_il      TIMESTAMPTZ  NOT NULL,
                               aggiornata_il  TIMESTAMPTZ  NOT NULL
);

-- l'elenco "le mie chat, dalla più recente": filtro + ordinamento coperti dallo stesso indice
CREATE INDEX idx_conversazione_proprietario ON conversazione (proprietario, aggiornata_il DESC);

CREATE TABLE messaggio (
                           id                UUID        PRIMARY KEY,
                           conversazione_id  UUID        NOT NULL REFERENCES conversazione (id) ON DELETE CASCADE,
                           ruolo             VARCHAR(12) NOT NULL CHECK (ruolo IN ('UTENTE', 'ASSISTENTE')),
                           testo             TEXT        NOT NULL,
                           fonti             TEXT,                                                 -- JSON, solo per ASSISTENTE
                           creato_il         TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_messaggio_conversazione ON messaggio (conversazione_id, creato_il);