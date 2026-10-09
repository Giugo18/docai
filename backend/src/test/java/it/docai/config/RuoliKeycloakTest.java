package it.docai.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.assertThat;


import java.util.List;
import java.util.Map;

public class RuoliKeycloakTest {

    @Test
    void iRuoliDelRealmDiventanoAuthorityConPrefissoRole() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("admin", "user")))
                .build();

        assertThat(SecurityConfig.ruoliKeycloak(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void senzaRealmAccessNessunRuolo() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none")
                .claim("sub", "qualcuno")
                .build();

        assertThat(SecurityConfig.ruoliKeycloak(jwt)).isEmpty();
    }
}
