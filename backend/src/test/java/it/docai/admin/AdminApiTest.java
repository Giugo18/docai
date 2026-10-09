package it.docai.admin;

import it.docai.TestcontainersConfiguration;
import it.docai.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;


import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public class AdminApiTest {

    @Autowired
    MockMvcTester mvc;

    /** Token finto con i ruoli Keycloak, convertiti dal NOSTRO convertitore. */
    private static RequestPostProcessor conRuoli(String sub, String... ruoli) {
        return jwt()
                .jwt(token -> token.subject(sub).claim("realm_access", Map.of("roles", List.of(ruoli))))
                .authorities(SecurityConfig::ruoliKeycloak);
    }

    @Test
    void adminVedeLeStatistiche() {
        assertThat(mvc.get().uri("/api/admin/statistiche").with(conRuoli("test-anna", "admin", "user")))
                .hasStatusOk();
    }

    @Test
    void utenteSenzaRuoloAdminRiceve403() {
        assertThat(mvc.get().uri("/api/admin/statistiche").with(conRuoli("test-mario", "user")))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.title").isEqualTo("Accesso negato");
    }

    @Test
    void senzaTokenRisponde401() {
        assertThat(mvc.get().uri("/api/admin/statistiche")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
