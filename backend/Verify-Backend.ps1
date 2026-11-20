param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$raizLou = $PSScriptRoot
if (-not $JavaHome) {
    $JavaHome = (Get-ChildItem "$raizLou/.tools" -Directory -Filter 'jdk-17*' -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
}
if (-not $JavaHome -or -not (Test-Path "$JavaHome/bin/java.exe")) { throw 'Indica -JavaHome con un JDK 17.' }
if (Get-NetTCPConnection -LocalPort 8095 -State Listen -ErrorAction SilentlyContinue) { throw 'El puerto 8095 está ocupado. Detén tu servidor manualmente antes de verificar.' }
$jarLou = "$raizLou/target/backend-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jarLou)) { throw 'Primero ejecuta mvnw.cmd verify con Java 17.' }
# Nunca abre ./data ni borra archivos. Cada ejecución conserva su propia H2 en disco.
$carpetaLou = Join-Path $raizLou "verification/$([guid]::NewGuid())"
New-Item -ItemType Directory -Path $carpetaLou | Out-Null
$dbLou = (Join-Path $carpetaLou 'persistencia').Replace('\','/')
$usuarioAnteriorLou = $env:LOU_ADMIN_USUARIO
$claveAnteriorLou = $env:LOU_ADMIN_CLAVE
$env:LOU_ADMIN_USUARIO = 'verificacion-local'
$env:LOU_ADMIN_CLAVE = [guid]::NewGuid().ToString('N')
$basicLou = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($env:LOU_ADMIN_USUARIO):$($env:LOU_ADMIN_CLAVE)"))
$cabecerasLou = @{ Authorization = "Basic $basicLou" }
$baseLou = 'http://127.0.0.1:8095'
$procesoLou = $null
$resultadosLou = [Collections.Generic.List[string]]::new()
function Confirmar-Lou([bool]$Condicion, [string]$Mensaje) {
    if (-not $Condicion) { throw "FALLÓ: $Mensaje" }
    $resultadosLou.Add($Mensaje)
    Write-Output "OK: $Mensaje"
}
function Iniciar-Lou([int]$Numero) {
    $argumentosLou = @('-jar', ('"{0}"' -f $jarLou), ('"--spring.datasource.url=jdbc:h2:file:{0};DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0"' -f $dbLou))
    $script:procesoLou = Start-Process -FilePath "$JavaHome/bin/java.exe" -ArgumentList $argumentosLou -WorkingDirectory $raizLou -WindowStyle Hidden -PassThru -RedirectStandardOutput "$carpetaLou/arranque-$Numero.log" -RedirectStandardError "$carpetaLou/error-$Numero.log"
    $limiteLou = (Get-Date).AddSeconds(90)
    while ((Get-Date) -lt $limiteLou) {
        if ($script:procesoLou.HasExited) { throw "El servidor terminó. Revisa $carpetaLou/arranque-$Numero.log" }
        try { $rLou = Invoke-WebRequest "$baseLou/api/productos" -UseBasicParsing -TimeoutSec 2; if ($rLou.StatusCode -eq 200) { return } } catch {}
        Start-Sleep -Milliseconds 300
    }
    throw 'Tiempo agotado al iniciar Spring Boot.'
}
function Detener-Lou {
    if ($script:procesoLou -and -not $script:procesoLou.HasExited) {
        # Únicamente el proceso Java que creó este script, nunca procesos de Android Studio.
        Stop-Process -Id $script:procesoLou.Id
        $script:procesoLou.WaitForExit()
    }
    $script:procesoLou = $null
}
function Esperar-CodigoLou([string]$Ruta, [string]$Metodo, [int]$Codigo, $Body = $null, [bool]$Autorizado = $false) {
    $pLou = @{ Uri = "$baseLou$Ruta"; Method = $Metodo; UseBasicParsing = $true; ContentType = 'application/json; charset=utf-8' }
    if ($Autorizado) { $pLou.Headers = $cabecerasLou }
    if ($null -ne $Body) { $pLou.Body = [Text.Encoding]::UTF8.GetBytes($Body) }
    try { $respuestaLou = Invoke-WebRequest @pLou; $codigoLou = [int]$respuestaLou.StatusCode }
    catch { if (-not $_.Exception.Response) { throw }; $codigoLou = [int]$_.Exception.Response.StatusCode }
    Confirmar-Lou ($codigoLou -eq $Codigo) "$Metodo $Ruta devuelve $Codigo"
}
try {
    Iniciar-Lou 1
    $inicialesLou = Invoke-RestMethod "$baseLou/api/productos"
    Confirmar-Lou (($inicialesLou.Count -eq 8) -and (($inicialesLou.id -join ',') -eq '1,2,3,4,5,6,7,8')) 'Ocho productos iniciales con IDs 1 al 8'
    Confirmar-Lou (($inicialesLou[5].disponible -eq $false) -and $inicialesLou[5].publicado) 'Disponibilidad independiente de publicación'
    $swaggerLou = Invoke-WebRequest "$baseLou/swagger-ui/index.html" -UseBasicParsing
    Confirmar-Lou (($swaggerLou.StatusCode -eq 200) -and $swaggerLou.Content.Contains('Swagger UI')) 'Swagger UI carga por HTTP'
    $jsLou = Invoke-WebRequest "$baseLou/swagger-ui/swagger-ui-bundle.js" -UseBasicParsing
    Confirmar-Lou ($jsLou.StatusCode -eq 200) 'JavaScript de Swagger carga por HTTP'
    $openapiLou = Invoke-RestMethod "$baseLou/v3/api-docs"
    Confirmar-Lou ($openapiLou.components.securitySchemes.basicAuth.scheme -eq 'basic') 'OpenAPI documenta HTTP Basic'
    $productoLou = @{ nombre='Producto de verificación'; descripcion='Pieza ficticia para comprobar persistencia real'; categoria='Interiores'; material='Algodón'; anchoCm=35; altoCm=60; precio=19990; disponible=$false; publicado=$true }
    $jsonLou = $productoLou | ConvertTo-Json -Compress
    Esperar-CodigoLou '/api/admin/productos' 'POST' 401 $jsonLou
    $creadoLou = Invoke-WebRequest "$baseLou/api/admin/productos" -Method Post -Headers $cabecerasLou -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($jsonLou)) -UseBasicParsing
    $idLou = ($creadoLou.Content | ConvertFrom-Json).id
    Confirmar-Lou (($creadoLou.StatusCode -eq 201) -and ($idLou -eq 9)) 'Crear producto devuelve 201 e ID 9 sin colisión'
    $productoLou.precio = 23990
    $editadoLou = Invoke-RestMethod "$baseLou/api/admin/productos/$idLou" -Method Put -Headers $cabecerasLou -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes(($productoLou | ConvertTo-Json -Compress)))
    Confirmar-Lou ($editadoLou.precio -eq 23990) 'Precio editado se refleja en API'
    Esperar-CodigoLou "/api/admin/productos/$idLou/publicacion" 'PATCH' 200 '{"publicado":false}' $true
    Esperar-CodigoLou "/api/productos/$idLou" 'GET' 404
    $todosLou = Invoke-RestMethod "$baseLou/api/admin/productos" -Headers $cabecerasLou
    Confirmar-Lou ($todosLou.Count -eq 9) 'Ocultar conserva el producto en administración'
    Esperar-CodigoLou '/api/admin/productos' 'POST' 400 '{"nombre":"","precio":-1}' $true
    # Se edita también una semilla para comprobar que el arranque no la sobrescribe.
    $semillaLou = $inicialesLou[0] | Select-Object nombre,descripcion,categoria,material,anchoCm,altoCm,precio,disponible,publicado
    $semillaLou.precio = 25990
    $null = Invoke-RestMethod "$baseLou/api/admin/productos/1" -Method Put -Headers $cabecerasLou -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes(($semillaLou | ConvertTo-Json -Compress)))
    Detener-Lou
    Iniciar-Lou 2
    $reinicioLou = Invoke-RestMethod "$baseLou/api/admin/productos" -Headers $cabecerasLou
    $recuperadoLou = $reinicioLou | Where-Object id -eq $idLou
    Confirmar-Lou (($reinicioLou.Count -eq 9) -and ($recuperadoLou.precio -eq 23990) -and (-not $recuperadoLou.publicado)) 'Primer reinicio conserva precio, ID y ocultación'
    Confirmar-Lou (($reinicioLou | Where-Object id -eq 1).precio -eq 25990) 'La semilla no sobrescribe ediciones administrativas'
    Esperar-CodigoLou "/api/admin/productos/$idLou/publicacion" 'PATCH' 200 '{"publicado":true}' $true
    $reactivadoLou = Invoke-RestMethod "$baseLou/api/productos/$idLou"
    Confirmar-Lou ((-not $reactivadoLou.disponible) -and $reactivadoLou.publicado) 'Republicar mantiene disponibilidad original'
    Esperar-CodigoLou '/api/admin/productos/8' 'DELETE' 204 $null $true
    Detener-Lou
    Iniciar-Lou 3
    $finalLou = Invoke-RestMethod "$baseLou/api/productos"
    Confirmar-Lou (($finalLou.id -join ',') -eq "1,2,3,4,5,6,7,$idLou") 'Segundo reinicio sin duplicados ni reposición de la semilla archivada'
    Confirmar-Lou (($finalLou | Where-Object id -eq $idLou).precio -eq 23990) 'Segundo reinicio conserva edición y reactivación'
    Esperar-CodigoLou '/api/productos/8' 'GET' 404
    $adminFinalLou = Invoke-RestMethod "$baseLou/api/admin/productos" -Headers $cabecerasLou
    Confirmar-Lou (($adminFinalLou.id -join ',') -eq "1,2,3,4,5,6,7,$idLou") 'El listado administrativo sigue excluyendo la semilla archivada'
    Esperar-CodigoLou '/api/admin/productos/8' 'DELETE' 404 $null $true
    $resultadosLou | Set-Content "$carpetaLou/resultados.txt" -Encoding UTF8
    Write-Output "APROBADAS: $($resultadosLou.Count). Evidencia y base aislada: $carpetaLou"
} finally {
    Detener-Lou
    $env:LOU_ADMIN_USUARIO = $usuarioAnteriorLou
    $env:LOU_ADMIN_CLAVE = $claveAnteriorLou
}
