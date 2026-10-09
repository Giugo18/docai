package it.docai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
//@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())   // API stateless con JWT: niente sessioni né cookie
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertitoreKeycloak())));
        return http.build();
    }

    /** Usa i ruoli di realm_access.roles al posto degli scope. */
    private static JwtAuthenticationConverter convertitoreKeycloak() {
        var convertitore = new JwtAuthenticationConverter();
        convertitore.setJwtGrantedAuthoritiesConverter(SecurityConfig::ruoliKeycloak);
        return convertitore;
    }

    /** "admin" in realm_access.roles diventa ROLE_ADMIN. */
    public static Collection<GrantedAuthority> ruoliKeycloak(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> ruoli)) {
            return List.of();
        }
        return ruoli.stream()
                .<GrantedAuthority>map(ruolo -> new SimpleGrantedAuthority("ROLE_" + ruolo.toString().toUpperCase()))
                .toList();
    }
}
