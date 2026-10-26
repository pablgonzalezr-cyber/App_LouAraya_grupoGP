$ErrorActionPreference = 'Stop'
$javaAnteriorLou = $env:JAVA_HOME
$directorioAnteriorLou = Get-Location
try {
    Set-Location $PSScriptRoot
    $jdkPortatilLou = Get-ChildItem -LiteralPath "$PSScriptRoot/.tools" -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($jdkPortatilLou) { $env:JAVA_HOME = $jdkPortatilLou.FullName }
    if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME/bin/java.exe")) { throw 'Configura JAVA_HOME hacia un JDK 17 antes de iniciar.' }
    $versionLou = & "$env:JAVA_HOME/bin/java.exe" --version | Out-String
    if ($versionLou -notmatch '^(openjdk|java) 17\.') { throw 'Este backend debe ejecutarse con Java 17. Configura JAVA_HOME hacia ese JDK.' }
    if (-not $env:LOU_ADMIN_USUARIO -or -not $env:LOU_ADMIN_CLAVE) { throw 'Define LOU_ADMIN_USUARIO y LOU_ADMIN_CLAVE en esta terminal. Consulta README.md.' }
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "Maven terminó con código $LASTEXITCODE" }
} finally {
    $env:JAVA_HOME = $javaAnteriorLou
    Set-Location $directorioAnteriorLou
}
