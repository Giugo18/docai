package it.docai.documento;

import it.docai.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class DocumentoRepositoryTest {

    @Autowired
    DocumentoRepository repository;

    @Test
    void riepilogoRaggruppaPerStatoSoloIDocumentiDelProprietario() {
        var a = Documento.nuovo("a.pdf", "application/pdf", 100, "mario");
        var b = Documento.nuovo("b.pdf", "application/pdf", 250, "mario");
        var c = Documento.nuovo("c.pdf", "application/pdf", 1_000, "mario");
        c.segnaIndicizzato();
        var diAnna = Documento.nuovo("d.pdf", "application/pdf", 5_000, "anna");
        repository.saveAll(List.of(a, b, c, diAnna));
        repository.flush();

        List<RigaRiepilogo> righe = repository.riepilogoPerStato("mario");

        Map<String, RigaRiepilogo> perStato = righe.stream()
                .collect(Collectors.toMap(RigaRiepilogo::getStato, riga -> riga));

        assertThat(perStato).containsOnlyKeys("CARICATO", "INDICIZZATO");
        assertThat(perStato.get("CARICATO").getNumero()).isEqualTo(2L);
        assertThat(perStato.get("CARICATO").getDimensione()).isEqualTo(350L);
        assertThat(perStato.get("INDICIZZATO").getNumero()).isEqualTo(1L);
        assertThat(perStato.get("INDICIZZATO").getDimensione()).isEqualTo(1_000L);
    }

    @Test
    void riepilogoVuotoPerUtenteSenzaDocumenti() {
        assertThat(repository.riepilogoPerStato("nessuno")).isEmpty();
    }
}