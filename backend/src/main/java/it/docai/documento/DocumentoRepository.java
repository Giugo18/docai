package it.docai.documento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
