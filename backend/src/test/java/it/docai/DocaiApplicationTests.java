package it.docai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Avvia l'intero contesto Spring contro un vero Postgres in Docker.
 * Flyway applica le migrazioni e Hibernate valida lo schema: se l'entità e la tabella
 * non coincidono, il test fallisce.
 */
@SpringBootTest
@Testcontainers
class DocaiApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    @Test
    void contestoSiAvvia() {
        // TODO settimana 3: aggiungi test sul repository e, con MockMvc + jwt(), sul controller
    }
}
