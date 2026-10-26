$ErrorActionPreference = 'Stop'
$herramientasLou = Join-Path $PSScriptRoot '.tools'
New-Item -ItemType Directory -Path $herramientasLou -Force | Out-Null
$existenteLou = Get-ChildItem $herramientasLou -Directory -Filter 'jdk-17*' | Where-Object { Test-Path "$($_.FullName)/bin/java.exe" } | Select-Object -First 1
if ($existenteLou) { Write-Output "JDK portátil existente: $($existenteLou.FullName)"; exit 0 }
$metadatosLou = Invoke-RestMethod 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse'
$paqueteLou = $metadatosLou[0].binary.package
$zipLou = Join-Path $herramientasLou 'jdk17.zip'
& curl.exe --fail --location --silent --show-error --output $zipLou $paqueteLou.link
if ($LASTEXITCODE -ne 0) { throw 'No se pudo descargar el JDK de Eclipse Adoptium.' }
if ((Get-FileHash $zipLou -Algorithm SHA256).Hash -ne $paqueteLou.checksum) { throw 'Checksum SHA-256 del JDK incorrecto. No se extraerá.' }
Expand-Archive -LiteralPath $zipLou -DestinationPath $herramientasLou
Write-Output 'JDK 17 portátil verificado. No se cambió JAVA_HOME global ni Android Studio.'
