# --- Stage 1: compila Keycloak con le opzioni di build ---
FROM quay.io/keycloak/keycloak:26.3.5 AS builder

ENV KC_DB=postgres
ENV KC_HEALTH_ENABLED=true

COPY themes/docai /opt/keycloak/themes/docai
RUN /opt/keycloak/bin/kc.sh build

# --- Stage 2: immagine finale, già compilata ---
FROM quay.io/keycloak/keycloak:26.3.5

COPY --from=builder /opt/keycloak/ /opt/keycloak/
COPY docai-realm.json /opt/keycloak/data/import/docai-realm.json

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]