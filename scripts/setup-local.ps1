param([switch]$Demo)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$destination=Join-Path $projectRoot '.env'
if(Test-Path -LiteralPath $destination){throw '.env already exists. Edit it explicitly; this script will not overwrite secrets.'}
function New-Secret { $bytes=New-Object byte[] 32; $rng=[System.Security.Cryptography.RandomNumberGenerator]::Create(); $rng.GetBytes($bytes); $rng.Dispose(); [Convert]::ToBase64String($bytes) }
$lines=@("POSTGRES_PASSWORD=$(New-Secret)","NOTIFICATION_DB_PASSWORD=$(New-Secret)","APP_JWT_SECRET=$(New-Secret)","ML_SERVICE_KEY=$(New-Secret)","GRAFANA_PASSWORD=$(New-Secret)","COOKIE_SECURE=false","DEMO_ENABLED=$($Demo.IsPresent.ToString().ToLower())","DEMO_PASSWORD=$(New-Secret)","DEMO_INITIAL_WALLET_BALANCE=$(if($Demo){50000}else{0})")
[System.IO.File]::WriteAllLines($destination,$lines,(New-Object System.Text.UTF8Encoding($false)))
Write-Output 'Created ignored .env. Demo email: demo@spendwise.local. Read DEMO_PASSWORD locally from .env.'

