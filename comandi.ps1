function NuovoToken {
  $r = Invoke-RestMethod -Method Post -Uri http://localhost:8180/realms/docai/protocol/openid-connect/token -Body @{
    client_id  = 'docai-frontend'
    grant_type = 'password'
    username   = 'mario'
    password   = 'mario'
  }
  return $r.access_token
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