# DocAI – Assistente documentale con Spring Boot 4 e Spring AI

Progetto di studio e portfolio: gli utenti si autenticano, caricano documenti PDF
e fanno domande in linguaggio naturale. L'assistente risponde citando i documenti.

## Stack

| Livello      | Tecnologia                                              |
|--------------|---------------------------------------------------------|
| Linguaggio   | Java 21                                                  |
| Backend      | Spring Boot 4.1, Spring Data JPA, Flyway                 |
| Sicurezza    | Spring Security (resource server JWT) + Keycloak        |
| Database     | PostgreSQL 17 + pgvector                                 |
| AI           | Spring AI 2.0, Ollama in locale (modello cloud opzionale)|
| Infrastruttura | Docker, Docker Compose, CI (settimana 13)              |
| Frontend     | Angular (settimana 13)                                   |

## Struttura

```
docai/
├── docker-compose.yml        Postgres+pgvector, Keycloak, Ollama, app
├── keycloak/                 realm "docai" importato all'avvio (utenti di prova)
└── backend/
    ├── Dockerfile            build multi-stage
    ├── pom.xml               dipendenze AI commentate: si attivano settimana per settimana
    └── src/main/java/it/docai/
        ├── config/           sicurezza
        ├── documento/        upload e metadati (entità, repository, service, controller)
        └── chat/             chat AI (da implementare)
```

Il codice è organizzato **per funzionalità** (documento, chat) e non per strato tecnico:
è l'approccio più usato oggi nei progetti Spring.

## Avvio

Prerequisiti: JDK 21, Maven, Docker.

```bash
# 1. infrastruttura
docker compose up -d postgres keycloak

# 2. applicazione (oppure dall'IDE)
cd backend
mvn spring-boot:run
```

In IntelliJ: aprire `backend/pom.xml` con *Open as Project*, impostare il JDK 21 e avviare
`DocaiApplication`. Senza Maven installato si può usare *Run Anything* (doppio Ctrl) con
`mvn spring-boot:run`.

Verifica: http://localhost:8080/actuator/health deve rispondere `{"status":"UP"}`.

Keycloak: http://localhost:8180 (admin/admin). Utenti di prova: `mario/mario` (user),
`anna/anna` (user + admin).

Database: `localhost:5432`, database `docai`, utente e password `docai`
(da riga di comando: `docker exec -it docai-postgres psql -U docai -d docai`).

### Provare le API

#### Linux / macOS (bash)

```bash
# ottieni un token
TOKEN=$(curl -s -X POST http://localhost:8180/realms/docai/protocol/openid-connect/token \
  -d grant_type=password -d client_id=docai-frontend \
  -d username=mario -d password=mario | jq -r .access_token)

# carica un documento
curl -H "Authorization: Bearer $TOKEN" -F "file=@manuale.pdf" http://localhost:8080/api/documenti

# elenca i tuoi documenti
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/documenti
```

#### Windows (PowerShell)

```powershell
# ottieni un token
$token = (Invoke-RestMethod -Method Post `
  -Uri http://localhost:8180/realms/docai/protocol/openid-connect/token `
  -Body @{ grant_type='password'; client_id='docai-frontend'; username='mario'; password='mario' }).access_token

# carica un documento
curl.exe -H "Authorization: Bearer $token" -F "file=@C:\percorso\manuale.pdf" http://localhost:8080/api/documenti

# elenca i tuoi documenti
Invoke-RestMethod -Uri http://localhost:8080/api/documenti -Headers @{ Authorization = "Bearer $token" }
```

#### Verifiche di sicurezza

- Senza token l'API risponde `401 Unauthorized`.
- Ogni utente vede solo i propri documenti: con il token di `anna` la lista dei documenti
  caricati da `mario` risulta vuota.

### Dalla settimana 6 (AI in locale)

```bash
docker compose --profile ai up -d
docker exec docai-ollama ollama pull llama3.2
docker exec docai-ollama ollama pull nomic-embed-text
```

Poi decommenta le dipendenze AI nel `pom.xml` e la sezione `spring.ai` in `application.yml`.

## Roadmap

I `TODO` nel codice indicano la settimana in cui affrontarli.

### Fase 1 – Fondamenta moderne
- [ ] **Sett. 1 – Java 21**: record, sealed interface, pattern matching, virtual thread
- [ ] **Sett. 2 – Spring Boot 4**: validazione dell'upload, `@RestControllerAdvice` con `ProblemDetail`
- [ ] **Sett. 3 – Dati**: test su repository e controller con Testcontainers e MockMvc
- [ ] **Sett. 4 – Sicurezza**: ruoli Keycloak (`realm_access.roles`), endpoint admin
- [ ] **Sett. 5 – Docker**: avvio completo con `--profile app`, healthcheck dell'app

### Fase 2 – AI engineering
- [ ] **Sett. 6 – Basi LLM**: `ChatClient`, prompt di sistema, streaming
- [ ] **Sett. 7 – Output strutturato**: estrazione dati da fattura PDF in un record Java
- [ ] **Sett. 8 – RAG, ingestione**: Tika → chunking → embedding → pgvector
- [ ] **Sett. 9 – RAG, interrogazione**: filtri per proprietario, citazione fonti, memoria chat
- [ ] **Sett. 10 – Tool calling**: il modello interroga il DB tramite i tuoi service
- [ ] **Sett. 11 – MCP e agenti**: esporre le funzioni dell'app come server MCP

### Fase 3 – Rendere tutto spendibile
- [ ] **Sett. 12 – Qualità**: valutazione delle risposte, costi, logging, ingestione asincrona
- [ ] **Sett. 13 – Frontend e deploy**: Angular, CI, deploy su cloud
- [ ] **Sett. 14 – Vetrina**: README finale, video demo, CV, LinkedIn

## Note

- Verificato su Windows 11 con Docker Desktop, JDK Temurin 21 e IntelliJ IDEA:
  compilazione, avvio, migrazione Flyway, login con Keycloak, upload ed elenco documenti.
- Le credenziali presenti nel progetto servono solo allo sviluppo in locale.
- Nella documentazione di Spring AI 2.0 verifica i nomi delle proprietà `spring.ai.*`:
  rispetto alla versione 1.x ci sono cambiamenti.
