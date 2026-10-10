-- I byte del file, separati dai metadati: si leggono solo quando servono
CREATE TABLE contenuto_documento (
                                     documento_id UUID  PRIMARY KEY REFERENCES documento (id) ON DELETE CASCADE,
                                     dati         BYTEA NOT NULL
);