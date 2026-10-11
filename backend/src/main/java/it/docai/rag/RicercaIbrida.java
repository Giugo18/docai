package it.docai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RicercaIbrida {

    private static final int CANDIDATI = 20;   // per ciascuna delle due classifiche
    private static final int RRF_K = 60;       // costante standard della Reciprocal Rank Fusion

    private static final String SQL = """
        WITH
        -- 1a. i più vicini per SIGNIFICATO (embedding, indice HNSW)
        semantica AS (
            SELECT id,
                   row_number() OVER (ORDER BY embedding <=> CAST(:vettore AS vector)) AS pos
            FROM vector_store
            WHERE metadata->>'proprietario' = :proprietario
              AND (CAST(:documentoId AS text) IS NULL OR metadata->>'documentoId' = :documentoId)
            ORDER BY embedding <=> CAST(:vettore AS vector)
            LIMIT :candidati
        ),
        -- 1b. i migliori per PAROLE (full-text, indice GIN); OR tra le parole, premia quelle vicine
        testuale AS (
            SELECT id,
                   row_number() OVER (ORDER BY frase DESC, rango DESC) AS pos
            FROM (
                SELECT v.id,
                       ts_rank_cd(v.contenuto_tsv, q) AS rango,
                       -- bonus: contiene almeno una sequenza di parole della domanda come frase
                       EXISTS (
                           SELECT 1 FROM unnest(CAST(:finestre AS text[])) AS f
                           WHERE numnode(phraseto_tsquery('italian', f)) > 1          -- almeno 2 parole vere
                             AND v.contenuto_tsv @@ phraseto_tsquery('italian', f)
                       ) AS frase
                FROM vector_store v,
                     to_tsquery('italian', replace(plainto_tsquery('italian', :domanda)::text, '&', '|')) AS q
                WHERE v.contenuto_tsv @@ q
                  AND v.metadata->>'proprietario' = :proprietario
                  AND (CAST(:documentoId AS text) IS NULL OR v.metadata->>'documentoId' = :documentoId)
            ) punteggi
            ORDER BY frase DESC, rango DESC
            LIMIT :candidati
                    ),
        -- 2. fusione RRF: 1/(60 + posizione) in ogni classifica in cui il chunk compare
        fusione AS (
            SELECT id,
                   sum(1.0 / (:rrfK + pos)) AS rrf,
                   bool_or(per_parole)      AS per_parole
            FROM (SELECT id, pos, false AS per_parole FROM semantica
                  UNION ALL
                  SELECT id, pos, true  AS per_parole FROM testuale) candidati
            GROUP BY id
        )
        -- 3. i migliori dopo la fusione, con i dati per le fonti
        SELECT v.id::text                               AS id,
               v.content                                AS testo,
               v.metadata->>'documentoId'               AS documento_id,
               v.metadata->>'nomeFile'                  AS nome_file,
               (v.metadata->>'pagina')::int             AS pagina,
               1 - (v.embedding <=> CAST(:vettore AS vector)) AS similarita,
               f.rrf,
               f.per_parole
        FROM fusione f
        JOIN vector_store v ON v.id = f.id
        WHERE f.per_parole OR 1 - (v.embedding <=> CAST(:vettore AS vector)) >= :soglia
        ORDER BY f.rrf DESC
        LIMIT :k
        """;

    private final JdbcClient jdbc;
    private final EmbeddingModel embeddingModel;

    public RicercaIbrida(JdbcClient jdbc, EmbeddingModel embeddingModel) {
        this.jdbc = jdbc;
        this.embeddingModel = embeddingModel;
    }

    public List<Document> cerca(String domanda, UUID documentoId, String proprietario, int k, double soglia) {
        String vettore = Arrays.toString(embeddingModel.embed(domanda));   // "[0.012, -0.034, ...]"

        return jdbc.sql(SQL)
                .param("vettore", vettore)
                .param("domanda", domanda)
                .param("finestre", finestre(domanda))
                .param("proprietario", proprietario)
                .param("documentoId", documentoId == null ? null : documentoId.toString())
                .param("candidati", CANDIDATI)
                .param("rrfK", RRF_K)
                .param("soglia", soglia)
                .param("k", k)
                .query((rs, riga) -> Document.builder()
                        .id(rs.getString("id"))
                        .text(rs.getString("testo"))
                        .metadata(Map.of(
                                "documentoId", rs.getString("documento_id"),
                                "nomeFile", rs.getString("nome_file"),
                                "pagina", rs.getInt("pagina"),
                                "rrf", rs.getDouble("rrf"),
                                "perParole", rs.getBoolean("per_parole")))
                        .score(rs.getDouble("similarita"))
                        .build())
                .list();
    }

    private static String[] finestre(String domanda) {
        String[] parole = domanda.toLowerCase().replaceAll("[^\\p{L}\\p{N}\\s]", " ").trim().split("\\s+");
        var risultato = new ArrayList<String>();
        for (int lunghezza = 2; lunghezza <= 3; lunghezza++) {
            for (int i = 0; i + lunghezza <= parole.length; i++) {
                risultato.add(String.join(" ", Arrays.copyOfRange(parole, i, i + lunghezza)));
            }
        }
        return risultato.toArray(String[]::new);
    }
}