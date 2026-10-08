package it.docai.documento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class DocumentoApiTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    private static final String MARIO = "test-mario";
    private static final String ANNA = "test-anna";

    @Autowired
    MockMvcTester mvc;

    /** Un JWT finto con il sub scelto: niente Keycloak nei test. */
    private static RequestPostProcessor utente(String sub) {
        return jwt().jwt(token -> token.subject(sub));
    }

    /** Carica un PDF tramite l'API e restituisce l'id del documento creato. */
    private String caricaPdf(String proprietario) throws Exception {
        var file = new MockMultipartFile("file", "prova.pdf", "application/pdf", "%PDF-1.4 prova".getBytes());
        MvcTestResult risposta = mvc.post().uri("/api/documenti")
                .multipart().file(file)
                .with(utente(proprietario))
                .exchange();
        assertThat(risposta).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(risposta.getResponse().getContentAsString(), "$.id");
    }

    private MvcTestResult rinomina(String id, String sub, String json) {
        return mvc.patch().uri("/api/documenti/{id}", id)
                .with(utente(sub))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    @Test
    void senzaTokenRisponde401() {
        assertThat(mvc.get().uri("/api/documenti")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rinominaConNomeVuotoRisponde400ConErrorePerCampo() throws Exception {
        String id = caricaPdf(MARIO);

        MvcTestResult risposta = rinomina(id, MARIO, """
                {"nomeFile": ""}
                """);

        assertThat(risposta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(risposta).bodyJson().extractingPath("$.title").isEqualTo("Richiesta non valida");
        assertThat(risposta).bodyJson().extractingPath("$.errori.nomeFile").isEqualTo("Il nome non può essere vuoto");
    }

    @Test
    void rinominaSenzaPdfRisponde400() throws Exception {
        String id = caricaPdf(MARIO);

        MvcTestResult risposta = rinomina(id, MARIO, """
                {"nomeFile": "dispensa"}
                """);

        assertThat(risposta).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(risposta).bodyJson().extractingPath("$.detail").isEqualTo("Il nome deve terminare con .pdf");
    }

    @Test
    void rinominaValidoAggiornaIlNome() throws Exception {
        String id = caricaPdf(MARIO);

        MvcTestResult risposta = rinomina(id, MARIO, """
                {"nomeFile": "Dispensa del corso.pdf"}
                """);

        assertThat(risposta).hasStatusOk();
        assertThat(risposta).bodyJson().extractingPath("$.nomeFile").isEqualTo("Dispensa del corso.pdf");

        // rilettura: il nome è davvero salvato nel database (dirty checking)
        assertThat(mvc.get().uri("/api/documenti/{id}", id).with(utente(MARIO)))
                .hasStatusOk()
                .bodyJson().extractingPath("$.nomeFile").isEqualTo("Dispensa del corso.pdf");
    }

    @Test
    void annaNonPuoRinominareUnDocumentoDiMario() throws Exception {
        String id = caricaPdf(MARIO);

        MvcTestResult risposta = rinomina(id, ANNA, """
                {"nomeFile": "rubato.pdf"}
                """);

        assertThat(risposta).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(risposta).bodyJson().extractingPath("$.title").isEqualTo("Documento non trovato");
    }
}
