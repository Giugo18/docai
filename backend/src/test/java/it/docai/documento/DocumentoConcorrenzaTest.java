package it.docai.documento;

import it.docai.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class DocumentoConcorrenzaTest {

    @Autowired
    DocumentoRepository repository;
    @Autowired
    DocumentoService service;

    @Test
    void loStatoDellIndicizzazioneNonCancellaLaRinomina() {
        var doc = repository.save(Documento.nuovo("prova.pdf", "application/pdf", 10, "mario"));

        service.rinomina(doc.getId(), "Dispensa.pdf", "mario");
        service.segnaErrore(doc.getId());                          // l'indicizzazione fallisce DOPO la rinomina

        var letto = repository.findById(doc.getId()).orElseThrow();
        assertThat(letto.getNomeFile()).isEqualTo("Dispensa.pdf");  // il nome è rimasto
        assertThat(letto.getStato()).isEqualTo(StatoDocumento.ERRORE);
        assertThat(letto.getTentativiIndicizzazione()).isEqualTo(1);
    }

    @Test
    void unaCopiaVecchiaNonPuoSovrascrivere() {
        var doc = repository.save(Documento.nuovo("prova.pdf", "application/pdf", 10, "mario"));
        var copiaVecchia = repository.findById(doc.getId()).orElseThrow();   // "fotografia" con versione 0

        service.rinomina(doc.getId(), "Dispensa.pdf", "mario");             // la riga passa a versione 1

        assertThatThrownBy(() -> repository.save(copiaVecchia))              // il vecchio modo di salvare
                .isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        assertThat(repository.findById(doc.getId()).orElseThrow().getNomeFile()).isEqualTo("Dispensa.pdf");
    }
}
