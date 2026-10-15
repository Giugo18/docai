# comandi.ps1 – funzioni di comodo per provare DocAI da PowerShell.
# Il file contiene SOLO definizioni di funzioni: caricarlo non esegue niente.
#
# Uso, nella console (una volta per sessione):
#   . .\comandi.ps1
#   $env:DOCAI_PWD = Read-Host "Password di mario"
#   NuovoToken                         # token di mario, aggiorna $token e lo copia negli appunti
#   NuovoToken anna                    # token di anna (chiede la password)
#   NuovoToken mario admin-cli         # token NON destinato a DocAI: Api deve dare 401
#   Api GET /api/documenti | Format-Table
#   Chiedi "domanda" [documentoId]
#   Assistente "domanda"

function NuovoToken($utente = 'mario', $client = 'docai-frontend') {
  # password: da $env:DOCAI_PWD per mario, altrimenti la chiede (mai scritta nel file)
  if ($utente -eq 'mario' -and $env:DOCAI_PWD) { $password = $env:DOCAI_PWD }
  else { $password = Read-Host "Password di $utente" }

  $r = Invoke-RestMethod -Method Post -Uri http://localhost:8180/realms/docai/protocol/openid-connect/token -Body @{
    client_id  = $client
    grant_type = 'password'
    username   = $utente
    password   = $password
  }
  $global:token = $r.access_token          # la usano Api, Chiedi e Assistente
  $global:token | Set-Clipboard            # pronto da incollare (Inspector, ecc.)

  $dati = Decodifica $global:token
  "Token di $utente per $client, aud: $($dati.aud), scade alle " +
    [DateTimeOffset]::FromUnixTimeSeconds($dati.exp).LocalDateTime.ToString('HH:mm:ss')
}

function Decodifica($jwt) {
  $p = $jwt.Split('.')[1].Replace('-','+').Replace('_','/')
  switch ($p.Length % 4) { 2 { $p += '==' } 3 { $p += '=' } }
  [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($p)) | ConvertFrom-Json
}

function Api($metodo, $percorso, $corpo) {
  $parametri = @{
    Method          = $metodo
    Uri             = "http://localhost:8080$percorso"
    Headers         = @{ Authorization = "Bearer $token" }
    UseBasicParsing = $true
  }
  if ($corpo) {
    $parametri.ContentType = 'application/json; charset=utf-8'
    $parametri.Body = [Text.Encoding]::UTF8.GetBytes(($corpo | ConvertTo-Json -Depth 5))
  }
  try {
    $r = Invoke-WebRequest @parametri -TimeoutSec 600
  }catch {
    $risposta = $_.Exception.Response
    if (-not $risposta) { "Errore: $($_.Exception.Message)"; return }     # backend spento, timeout…
    $stato = $risposta.StatusCode.value__
    $id = $risposta.Headers['X-Request-Id']
    # PowerShell 5.1 non legge il corpo di application/problem+json: lo leggo dallo stream, in UTF-8
    $lettore = New-Object IO.StreamReader($risposta.GetResponseStream(), [Text.Encoding]::UTF8)
    $corpo = $lettore.ReadToEnd()
    $dettaglio = try { ($corpo | ConvertFrom-Json).detail } catch { $corpo }
    "Errore $stato [id $id]: $dettaglio"
    return
  }
  $testo = [Text.Encoding]::UTF8.GetString($r.RawContentStream.ToArray())
  if ($testo) { $testo | ConvertFrom-Json }
}

function Chiedi($testo, $documentoId) {
  $corpo = @{ domanda = $testo }
  if ($documentoId) { $corpo.documentoId = $documentoId }
  $orologio = [Diagnostics.Stopwatch]::StartNew()
  $dati = Api POST /api/rag/domanda $corpo
  $orologio.Stop()
  if ($dati -is [string]) { $dati; return }
  "`n$($dati.risposta)`n"
  "Tempo: {0:N1} s" -f $orologio.Elapsed.TotalSeconds
  $dati.fonti | Format-Table
}

function Assistente($testo) {
  $dati = Api POST /api/assistente @{ domanda = $testo }
  if ($dati -is [string]) { $dati; return }
  "`n$($dati.risposta)`n"
}

function Valutazione {
  NuovoToken valutatore | Out-Null
  $parametri = @{
    Method          = 'Post'
    Uri             = 'http://localhost:8080/api/rag/valutazione'
    Headers         = @{ Authorization = "Bearer $global:token" }
    UseBasicParsing = $true
    TimeoutSec      = 900
  }
  try {
    $risposta = Invoke-WebRequest @parametri
  } catch {
    "Errore $($_.Exception.Response.StatusCode.value__): guarda il log del backend"
    return
  }
  # PowerShell 5.1 decodifica come Latin-1: rileggo i byte come UTF-8
  $json = [Text.Encoding]::UTF8.GetString($risposta.RawContentStream.ToArray())
  $global:valutazione = $json | ConvertFrom-Json
  $json | Out-File -Encoding utf8 .\valutazione-ultima.json
  $global:valutazione | Select-Object casi, fontiTrovate, fontiCitate, citazioniValide, risposteCorrette, tempoMedio
}