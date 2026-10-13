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

$env:DOCAI_PWD = Read-Host "Password di mario"   # una volta per sessione
. .\comandi.ps1
NuovoToken
Api GET /api/documenti | Format-Table

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
  } catch {
    "Errore $($_.Exception.Response.StatusCode.value__): guarda il log del backend"
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