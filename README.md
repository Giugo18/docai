# DocAI – Assistente documentale con Spring Boot 4, Spring AI e Angular

Progetto di studio e portfolio: gli utenti si autenticano con Keycloak, caricano documenti PDF
e (dalla fase 2) fanno domande in linguaggio naturale. L'assistente risponde citando i documenti.

## Stack

| Livello        | Tecnologia                                                        |
|----------------|-------------------------------------------------------------------|
| Linguaggio     | Java 21 (record, sealed interface, pattern matching, virtual thread) |
| Backend        | Spring Boot 4.1, Spring Data JPA, Flyway, Bean Validation, ProblemDetail (RFC 9457) |
| Sicurezza      | Spring Security (resource server JWT) + Keycloak 26 (PKCE, tema di login personalizzato) |
| Database       | PostgreSQL 17 + pgvector                                          |
| Frontend       | Angular 22 (standalone, signals), Tailwind CSS 4, keycloak-angular |
| Test           | JUnit 6, MockMvcTester, Spring Security Test, Testcontainers 2    |
| AI             | Spring AI 2.0, Ollama in locale (dalla settimana 6)               |
| Infrastruttura | Docker, Docker Compose                                            |

## Struttura

```
docai/
├── docker-compose.yml          Postgres+pgvector, Keycloak, Ollama, app
├── keycloak/
│   ├── docai-realm.json        realm "docai" importato all'avvio (client, utenti di prova, tema, lingue)
│   └── themes/docai/login/     tema della pagina di login (CSS e testi in italiano)
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/it/docai/
│       │   ├── config/         sicurezza
│       │   ├── documento/      upload, elenco, dettaglio, rinomina, riepilogo
│       │   ├── errori/         GestoreErrori → ProblemDetail
│       │   ├── demo/           endpoint di prova (solo profilo dev)
│       │   └── chat/           chat AI (da implementare)
│       └── test/java/it/docai/ test d'integrazione con Testcontainers
└── frontend/                   Angular 22 + Tailwind
    ├── proxy.conf.json         /api → http://localhost:8080 in sviluppo
    └── src/app/
        ├── documenti/          pagina, riepilogo, caricamento, elenco, dettaglio
        └── errori/             lettura dei ProblemDetail del backend
```

Il codice è organizzato **per funzionalità** (documento, chat) e non per strato tecnico.

## Avvio in locale

Prerequisiti: JDK 21, Maven (o IntelliJ), Node.js 24, Docker Desktop.

### 1. Infrastruttura

```powershell
docker compose up -d postgres keycloak
```

- Keycloak: http://localhost:8180 (console admin: `admin` / `admin`)
- Utenti di prova del realm `docai`: `mario` / `mario` (ruolo user), `anna` / `anna` (user + admin)
- Database: `localhost:5432`, database, utente e password `docai`
  (`docker exec -it docai-postgres psql -U docai -d docai`)

### 2. Backend

In IntelliJ: aprire `backend/pom.xml` come progetto, JDK 21, avviare `DocaiApplication`.
Da riga di comando: `cd backend` e `mvn spring-boot:run`.

Verifica: http://localhost:8080/actuator/health → `{"status":"UP"}`.

### 3. Frontend

```powershell
cd frontend
npm install
npm start
```

Aprire http://localhost:4200: si viene reindirizzati al login di Keycloak.
In sviluppo le chiamate a `/api` passano dal proxy di `ng serve` verso `localhost:8080`
(niente CORS).

> Su Windows, se PowerShell blocca `npm`/`ng`:
> `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`

## API

| Metodo | Percorso                     | Descrizione                              |
|--------|------------------------------|------------------------------------------|
| GET    | `/api/documenti`             | documenti dell'utente, dal più recente   |
| POST   | `/api/documenti`             | upload (multipart, campo `file`, solo PDF, max 10 MB) → 201 |
| GET    | `/api/documenti/riepilogo`   | totale, spazio usato, conteggio per stato |
| GET    | `/api/documenti/{id}`        | dettaglio (404 se non esiste o è di un altro utente) |
| PATCH  | `/api/documenti/{id}`        | rinomina, body `{"nomeFile": "....pdf"}` (Bean Validation) |

Gli errori sono restituiti come `application/problem+json` (RFC 9457), con `title`, `detail`
e, per la validazione, la mappa `errori` campo → messaggio.

### Provare le API da PowerShell

```powershell
# token di mario
$token = (Invoke-RestMethod -Method Post `
  -Uri http://localhost:8180/realms/docai/protocol/openid-connect/token `
  -Body @{ grant_type='password'; client_id='docai-frontend'; username='mario'; password='mario' }).access_token

# elenco
Invoke-RestMethod -Uri http://localhost:8080/api/documenti -Headers @{ Authorization = "Bearer $token" }

# upload
curl.exe -i -H "Authorization: Bearer $token" -F "file=@C:\percorso\manuale.pdf" http://localhost:8080/api/documenti

# rinomina: per i body JSON usare Invoke-RestMethod + ConvertTo-Json
$body = @{ nomeFile = 'Dispensa del corso.pdf' } | ConvertTo-Json
Invoke-RestMethod -Method Patch -Uri "http://localhost:8080/api/documenti/<id>" `
  -Headers @{ Authorization = "Bearer $token" } -ContentType 'application/json; charset=utf-8' -Body $body
```

Note per Windows PowerShell 5.1:

- con `curl.exe` usare `-i` (mostra status e header); con `-s` un 401 senza corpo non stampa nulla;
- un JSON con spazi passato a `curl.exe` viene spezzato in più argomenti: per i body JSON
  usare `Invoke-RestMethod` con `ConvertTo-Json`;
- i token durano 5 minuti: in caso di 401 rigenerarlo.

## Test

```powershell
cd backend
mvn test
```

oppure in IntelliJ: tasto destro su `src/test/java` → *Run 'All Tests'*.

Serve **Docker Desktop avviato**: Testcontainers crea un Postgres `pgvector/pgvector:pg17`
usa e getta per i test (porta casuale, migrazioni Flyway applicate da zero) e lo elimina alla fine.
Il database del docker-compose non viene usato né toccato.

| Classe                    | Tipo                                         | Cosa verifica |
|---------------------------|----------------------------------------------|---------------|
| `DocaiApplicationTests`   | `@SpringBootTest`                            | il contesto completo si avvia (smoke test) |
| `DocumentoApiTest`        | `@SpringBootTest` + `MockMvcTester` + `jwt()` | 401 senza token, 400 di validazione con errori per campo, rinomina salvata, 404 su documento di un altro utente |
| `DocumentoRepositoryTest` | `@DataJpaTest`                               | query nativa del riepilogo: raggruppamento per stato, filtro per proprietario, somme |

La configurazione del container è condivisa in `TestcontainersConfiguration` e importata con `@Import`.

## Keycloak

Il realm è definito in `keycloak/docai-realm.json` e importato **solo se non esiste**
(`start-dev --import-realm`). Contiene:

- client pubblico `docai-frontend` con PKCE S256 e post logout redirect;
- utenti di prova con **ID fissi**: il backend usa l'ID (`sub` del token) come proprietario dei
  documenti, quindi ricreando il container gli utenti devono mantenere lo stesso ID;
- tema di login `docai` (estende `keycloak.v2`), lingua italiana di default.

In `start-dev` i dati di Keycloak vivono dentro il container: dopo una modifica al JSON o al
`docker-compose.yml`, `docker compose up -d keycloak` ricrea il container e reimporta il realm.
Le modifiche al CSS e ai testi del tema sono visibili subito (F5), senza riavvio.

## Dalla settimana 6 (AI in locale)

```powershell
docker compose --profile ai up -d
docker exec docai-ollama ollama pull llama3.2
docker exec docai-ollama ollama pull nomic-embed-text
```

Poi decommentare le dipendenze AI nel `pom.xml` e la sezione `spring.ai` in `application.yml`.

## Roadmap

### Fase 1 – Fondamenta moderne
- [x] **Sett. 1 – Java 21**: record, sealed interface, pattern matching, virtual thread
- [x] **Sett. 2 – Spring Boot 4**: validazione dell'upload, `@RestControllerAdvice` con `ProblemDetail`, Bean Validation
- [x] **Frontend**: Angular 22, login Keycloak con PKCE, interceptor, routing, Tailwind e layout responsive
- [x] **Login personalizzato**: tema Keycloak con i colori dell'app, italiano
- [x] **Sett. 3 – Test**: MockMvcTester, Spring Security Test, Testcontainers, `@DataJpaTest`
- [ ] **Sett. 4 – Sicurezza**: `issuer-uri`, ruoli Keycloak (`realm_access.roles`), endpoint admin, 403 come ProblemDetail
- [ ] **Sett. 5 – Docker**: avvio completo con `--profile app`, healthcheck, frontend su nginx
- [ ] **Deploy online** in HTTPS e **PWA** installabile su smartphone

### Fase 2 – AI engineering
- [ ] **Sett. 6 – Basi LLM**: `ChatClient`, prompt di sistema, streaming
- [ ] **Sett. 7 – Output strutturato**: estrazione dati da fattura PDF in un record Java
- [ ] **Sett. 8 – RAG, ingestione**: Tika → chunking → embedding → pgvector
- [ ] **Sett. 9 – RAG, interrogazione**: filtri per proprietario, citazione fonti, memoria chat
- [ ] **Sett. 10 – Tool calling**: il modello interroga il DB tramite i service
- [ ] **Sett. 11 – MCP e agenti**: esporre le funzioni dell'app come server MCP

### Fase 3 – Rendere tutto spendibile
- [ ] **Sett. 12 – Qualità**: valutazione delle risposte, costi, logging, ingestione asincrona
- [ ] **Sett. 13 – CI**: GitHub Actions con i test Testcontainers e la build del frontend
- [ ] **Sett. 14 – Vetrina**: README finale, video demo, CV, LinkedIn

## Note

- Verificato su Windows 11 con Docker Desktop, JDK Temurin 21, IntelliJ IDEA e VS Code.
- Le credenziali presenti nel progetto servono solo allo sviluppo in locale.
- Nella documentazione di Spring AI 2.0 verificare i nomi delle proprietà `spring.ai.*`:
  rispetto alla versione 1.x ci sono cambiamenti.
