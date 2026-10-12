package it.docai.strumenti;

import it.docai.TestcontainersConfiguration;
import it.docai.documento.DocumentoDto;
import it.docai.documento.DocumentoService;
import it.docai.rag.Fonte;
import it.docai.rag.Proposta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional                       // rollback a fine test: niente dati residui, e niente indicizzazione (AFTER_COMMIT)
class StrumentiSicurezzaTest {

    private static final String MARIO = "test-mario";
    private static final String ANNA = "test-anna";
    private static final int DIMENSIONI = 1024;

    /** Un documento creato nel test: id, proprietario e nome. */
    private record Doc(UUID id, String proprietario, String nomeFile) {}

    @MockitoBean EmbeddingModel embeddingModel;    // niente Ollama: ogni testo → lo stesso vettore

    @Autowired StrumentiDocumenti strumentiDocumenti;
    @Autowired StrumentiRicerca strumentiRicerca;
    @Autowired StrumentiModifica strumentiModifica;
    @Autowired DocumentoService documentoService;
    @Autowired JdbcClient jdbc;

    private Doc diMario;
    private Doc diAnna;

    @BeforeEach
    void prepara() throws Exception {
        float[] vettore = new float[DIMENSIONI];
        Arrays.fill(vettore, 0.1f);
        when(embeddingModel.embed(anyString())).thenReturn(vettore);

        diMario = carica(MARIO, "fattura-mario.pdf");
        diAnna = carica(ANNA, "fattura-anna.pdf");

        // stesso testo, stesso vettore: a separarli resta solo il proprietario
        passaggio(diMario, "Fattura tipografia: totale 451,40 euro");
        passaggio(diAnna, "Fattura tipografia: totale 999,00 euro, documento riservato di anna");
    }

    @Test
    void elencaDocumenti_restituisceSoloQuelliDelProprietario() {
        var elenco = strumentiDocumenti.elencaDocumenti(contesto(MARIO, null));

        assertThat(elenco).extracting(StrumentiDocumenti.DocumentoInBreve::nomeFile)
                .contains("fattura-mario.pdf")
                .doesNotContain("fattura-anna.pdf");
    }

    @Test
    void cercaNeiDocumenti_nonRestituisceMaiPassaggiDiAltriUtenti() {
        List<Fonte> fonti = new ArrayList<>();
        var passaggi = strumentiRicerca.cercaNeiDocumenti(
                "fattura tipografia totale riservato anna", contesto(MARIO, null, fonti, new ArrayList<>()));

        assertThat(passaggi).isNotEmpty();
        assertThat(passaggi).extracting(StrumentiRicerca.Passaggio::nomeFile).containsOnly("fattura-mario.pdf");
        assertThat(fonti).extracting(Fonte::nomeFile).containsOnly("fattura-mario.pdf");
    }

    @Test
    void cercaNeiDocumenti_conDocumentoDiAltroUtente_nonTrovaNiente() {
        var passaggi = strumentiRicerca.cercaNeiDocumenti(
                "fattura tipografia totale", contesto(MARIO, diAnna.id()));

        assertThat(passaggi).isEmpty();
    }

    @Test
    void proponiRinomina_suDocumentoDiAltroUtente_vieneRifiutata() {
        List<Proposta> proposte = new ArrayList<>();
        String esito = strumentiModifica.proponiRinomina(
                diAnna.id().toString(), "rubata.pdf", contesto(MARIO, null, new ArrayList<>(), proposte));

        assertThat(esito).contains("non trovato");
        assertThat(proposte).isEmpty();
        assertThat(nomeNelDatabase(diAnna)).isEqualTo("fattura-anna.pdf");
    }

    @Test
    void proponiRinomina_creaLaPropostaMaNonModificaIlDatabase() {
        List<Proposta> proposte = new ArrayList<>();
        strumentiModifica.proponiRinomina(
                diMario.id().toString(), "nuovo-nome.pdf", contesto(MARIO, null, new ArrayList<>(), proposte));

        assertThat(proposte).singleElement().satisfies(p -> {
            assertThat(p.documentoId()).isEqualTo(diMario.id().toString());
            assertThat(p.nuovoNome()).isEqualTo("nuovo-nome.pdf");
        });
        assertThat(nomeNelDatabase(diMario)).isEqualTo("fattura-mario.pdf");   // solo l'utente, confermando, lo cambia
    }

    @Test
    void proponiRinomina_senzaEstensionePdf_vieneRifiutata() {           // ← nuovo
        List<Proposta> proposte = new ArrayList<>();
        String esito = strumentiModifica.proponiRinomina(
                diMario.id().toString(), "Fattura ottobre", contesto(MARIO, null, new ArrayList<>(), proposte));

        assertThat(esito).contains(".pdf");
        assertThat(proposte).isEmpty();
    }

    // ---------- supporto ----------

    private static ToolContext contesto(String proprietario, UUID documentoId) {
        return contesto(proprietario, documentoId, new ArrayList<>(), new ArrayList<>());
    }

    private static ToolContext contesto(String proprietario, UUID documentoId,
                                        List<Fonte> fonti, List<Proposta> proposte) {
        return new ToolContext(ContestoStrumenti.di(proprietario, documentoId, fonti, proposte));
    }

    /** Come l'upload vero, con lo stesso finto PDF di DocumentoApiTest. */
    private Doc carica(String proprietario, String nomeFile) throws Exception {
        var file = new MockMultipartFile("file", nomeFile, "application/pdf", "%PDF-1.4 prova".getBytes());
        DocumentoDto dto = documentoService.carica(file, proprietario);       // ADATTA l'ordine dei parametri
        return new Doc(dto.id(), proprietario, dto.nomeFile());
    }

    private void passaggio(Doc d, String testo) {
        String metadati = """
                {"documentoId":"%s","proprietario":"%s","nomeFile":"%s","pagina":1}"""
                .formatted(d.id(), d.proprietario(), d.nomeFile());
        jdbc.sql("INSERT INTO vector_store (id, content, metadata, embedding) VALUES (?, ?, ?::json, ?::vector)")
                .params(UUID.randomUUID(), testo, metadati, vettoreSql())
                .update();
    }

    private static String vettoreSql() {
        return IntStream.range(0, DIMENSIONI).mapToObj(i -> "0.1")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String nomeNelDatabase(Doc d) {
        return documentoService.trovaDelProprietario(d.id(), d.proprietario()).getNomeFile();
    }
}