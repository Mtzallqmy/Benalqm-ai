param(
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$ComposeArgs = @("up", "-d", "--build")
)

$RepoRoot = Split-Path -Parent $PSScriptRoot
$EnvFile = if ($env:MOATAZ_ENV_FILE) { $env:MOATAZ_ENV_FILE } else { Join-Path $RepoRoot ".env.moataz" }
if (-not (Test-Path $EnvFile)) {
  Write-Error "Missing $EnvFile. Copy .env.moataz.example to .env.moataz and configure it."
  exit 2
}
Push-Location (Join-Path $RepoRoot "docker")
try {
  docker compose --env-file $EnvFile -f docker-compose.yaml -f docker-compose.moataz.yaml @ComposeArgs
} finally {
  Pop-Location
}
