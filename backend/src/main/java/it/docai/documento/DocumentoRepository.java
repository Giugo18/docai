package it.docai.documento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<Documento, UUID> {

    List<Documento> findByProprietarioOrderByCaricatoIlDesc(String proprietario);

    @Query(value = """
        SELECT stato,
               COUNT(*)                AS numero,
               SUM(dimensione)::bigint AS dimensione
        FROM documento
        WHERE proprietario = :proprietario
        GROUP BY stato
        """, nativeQuery = true)
    List<RigaRiepilogo> riepilogoPerStato(String proprietario);

    Optional<Documento> findByIdAndProprietario(UUID id, String proprietario);

    @Query("""
        SELECT d.proprietario AS proprietario,
               COUNT(d)       AS documenti,
               SUM(d.dimensione) AS dimensione
        FROM Documento d
        GROUP BY d.proprietario
        ORDER BY COUNT(d) DESC
        """)
    List<RigaStatistica> statistichePerProprietario();

    List<Documento> findByStato(StatoDocumento stato);
    List<Documento> findByStatoIn(Collection<StatoDocumento> stati);
    List<Documento> findByStatoAndTentativiIndicizzazioneLessThan(StatoDocumento stato, int massimo);

    // ===== Modifiche mirate: ognuna scrive SOLO le sue colonne =====
    // Le UPDATE scritte a mano ignorano @Version: la versione va incrementata qui,
    // così un save() fatto dopo su una copia vecchia fallisce come deve.
    // clearAutomatically: dopo l'UPDATE le entità in memoria sono vecchie, si rileggono dal DB.

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Documento d
        SET d.stato = :stato, d.versione = d.versione + 1
        WHERE d.id = :id
        """)
    int aggiornaStato(UUID id, StatoDocumento stato);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Documento d
        SET d.stato = :stato, d.tentativiIndicizzazione = 0, d.versione = d.versione + 1
        WHERE d.id = :id
        """)
    int segnaIndicizzato(UUID id, StatoDocumento stato);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Documento d
        SET d.stato = :stato,
            d.tentativiIndicizzazione = d.tentativiIndicizzazione + 1,
            d.versione = d.versione + 1
        WHERE d.id = :id
        """)
    int segnaErrore(UUID id, StatoDocumento stato);

    /** Il proprietario nel WHERE è la protezione IDOR: 0 righe = non esiste o non è suo. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Documento d
        SET d.nomeFile = :nome, d.versione = d.versione + 1
        WHERE d.id = :id AND d.proprietario = :proprietario
        """)
    int rinomina(UUID id, String proprietario, String nome);

    /** DELETE mirata: non dipende dalla versione, quindi si può eliminare anche durante l'indicizzazione. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Documento d WHERE d.id = :id AND d.proprietario = :proprietario")
    int eliminaDelProprietario(UUID id, String proprietario);
}