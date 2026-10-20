# Funzioni per provare l'API di DocAI in locale (backend su 8080, Keycloak di sviluppo su 8180).
# Caricarle nella sessione:   . .\strumenti\docai-test.ps1     (il punto iniziale e lo spazio servono)
# Oppure per sempre:          notepad $PROFILE   e incollare questo contenuto

$DocaiApi      = 'http://localhost:8080'
$DocaiKeycloak = 'http://localhost:8180/realms/docai/protocol/openid-connect/token'

# Chiede un token a Keycloak e lo salva in $token.
# NON assegnarla: "NuovoToken valutatore", non "$token = NuovoToken valutatore".
function NuovoToken([string]$utente = 'mario', [string]$client = 'docai-frontend') {
  $sicura = Read-Host "Password di $utente" -AsSecureString          # non si vede mentre la scrivi
  $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sicura)
  try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    $r = Invoke-RestMethod -Method Post -Uri $DocaiKeycloak -Body @{
      client_id  = $client
      grant_type = 'password'
      username   = $utente
      password   = $password
    }
    $global:token = $r.access_token
    Write-Host "Token di $utente per $client, valido $($r.expires_in) secondi" -ForegroundColor Green
  } catch {
    $global:token = $null
    Write-Host "Login fallito per ${utente}: $($_.ErrorDetails.Message)" -ForegroundColor Red
    Write-Host "Controlla password, utente attivo, Required actions vuote e Direct access grants attivo (solo in dev)."
  } finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)            # cancella la password dalla memoria
  }
}

# Chiama l'API con il token: Api GET /api/documenti   oppure   Api POST /percorso @{ chiave = 'valore' }
function Api([string]$metodo, [string]$percorso, $corpo) {
  if (-not $global:token) { Write-Host "Nessun token: prima NuovoToken" -ForegroundColor Yellow; return }
  $parametri = @{
    Method          = $metodo
    Uri             = "$DocaiApi$percorso"
    Headers         = @{ Authorization = "Bearer $global:token" }
    UseBasicParsing = $true
    TimeoutSec      = 900                                            # la valutazione dura minuti
  }
  if ($corpo) {
    $parametri.ContentType = 'application/json; charset=utf-8'
    $parametri.Body = [Text.Encoding]::UTF8.GetBytes(($corpo | ConvertTo-Json -Depth 5))
  }
  try {
    $r = Invoke-WebRequest @parametri
  } catch {
    $stato = $_.Exception.Response.StatusCode.value__
    $dettaglio = $_.ErrorDetails.Message
    try {
      $pd = $dettaglio | ConvertFrom-Json                              # ProblemDetail del backend
      Write-Host "Errore $stato [id $($pd.idRichiesta)]: $($pd.detail)" -ForegroundColor Red
    } catch {
      Write-Host "Errore ${stato}: $dettaglio (guarda il log del backend)" -ForegroundColor Red
    }
    return
  }
  $testo = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
  if ($testo) { $testo | ConvertFrom-Json }
}

# Domanda RAG (pagina Chiedi), con tempo e fonti: Chiedi "a cosa serve Caddy?"
function Chiedi([string]$testo, [string]$documentoId) {
  $corpo = @{ domanda = $testo }
  if ($documentoId) { $corpo.documentoId = $documentoId }
  $orologio = [Diagnostics.Stopwatch]::StartNew()
  $dati = Api POST /api/rag/domanda $corpo
  $orologio.Stop()
  if (-not $dati) { return }
  "`n$($dati.risposta)`n"
  "Tempo: {0:N1} s" -f $orologio.Elapsed.TotalSeconds
  $dati.fonti | Format-Table numero, nomeFile, pagina, punteggio
}

# Il token contiene i dati giusti? Mostra iss, aud, azp, utente e scadenza (la firma non serve leggerla)
function InfoToken {
  if (-not $global:token) { Write-Host "Nessun token" -ForegroundColor Yellow; return }
  $p = $global:token.Split('.')[1].Replace('-', '+').Replace('_', '/')
  while ($p.Length % 4) { $p += '=' }
  [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($p)) | ConvertFrom-Json |
    Select-Object iss, aud, azp, preferred_username, @{ n = 'scade'; e = { [DateTimeOffset]::FromUnixTimeSeconds($_.exp).LocalDateTime } }
}

# Valutazione completa (serve VALUTAZIONE_ATTIVA=true): mostra il riepilogo e i casi falliti
# Valutazione (serve VALUTAZIONE_ATTIVA=true). Tutti i casi: Valuta
# Una parte: Valuta -modalita AGENTE   |   Valuta -contiene java   |   Valuta ASSISTENTE tokyo
function Valuta([string]$modalita, [string]$contiene) {
  $parametri = @()
  if ($modalita) { $parametri += "modalita=$($modalita.ToUpper())" }
  if ($contiene) { $parametri += "contiene=$([uri]::EscapeDataString($contiene))" }
  $percorso = '/api/rag/valutazione' + $(if ($parametri) { '?' + ($parametri -join '&') } else { '' })

  $global:report = Api POST $percorso
  if (-not $global:report) { return }
  $global:report | Select-Object * -ExcludeProperty esiti | Format-List
  $falliti = $global:report.esiti | Where-Object { -not $_.rispostaCorretta -or -not $_.fonteCitata }
  if ($falliti) { $falliti | Format-List } else { Write-Host "Tutti i casi passati" -ForegroundColor Green }
}