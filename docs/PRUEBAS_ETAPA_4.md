# Etapa 4: productos, colecciones y campañas

## Implementación y decisiones

Se revisaron Producto, ProductosDemo, CatalogoViewModel, CatalogoScreen,
FavoritosRepository, FavoritosDatabase y AppDatabase antes de implementar.
La rama de trabajo es feature/cotizaciones. No se añadieron dependencias.

Productos ahora es una tabla Room de AppDatabase. Se mantienen los ocho IDs
históricos 1–8 y los campos del modelo original. El catálogo, sus filtros,
tarjetas, vista de favoritos y detalle se adaptan a un Flow persistente; no se
reconstruyen. Los productos nuevos usan IDs autogenerados mayores que los iniciales.

Administración permite buscar, crear, editar, publicar/despublicar y desactivar.
La disponibilidad distingue Disponible de A pedido: ambos pueden publicarse.
La marca activo controla la publicación del producto en el catálogo cliente.
Desactivar nunca borra el registro. Puede reactivarse desde Editar.
Los favoritos de productos desactivados se conservan en su base; no se muestran
al cliente mientras el producto esté oculto y reaparecen al publicarlo otra vez.

Validaciones: nombre 2–100 caracteres, descripción 10–2000, categoría conocida,
material 2–100, ancho y largo/alto enteros de 1–10000 cm y precio referencial
entero de 1–1000000000 CLP. Los formularios muestran errores por campo y bloquean
envíos repetidos mientras guardan. La desactivación requiere confirmación.

Las imágenes opcionales se seleccionan con el selector de documentos del sistema,
sin cámara ni permiso general de almacenamiento. Se copia el archivo a almacenamiento
privado de la aplicación (máximo 10 MB, imagen reconocida y dimensiones hasta
16000 px); la visualización reduce resolución para limitar memoria. Los nombres
drawable existentes y el marcador de fotografía pendiente siguen funcionando.

Colecciones: nombre, descripción, activa/publicada y asociación de productos
existentes, con actualización transaccional de las asociaciones. Para publicar
se requiere al menos un producto asociado. Los miembros desactivados no se muestran
al cliente, aunque sus asociaciones se conservan.

Campañas: título, descripción, inicio, término, publicación y activación.
Fechas estrictas dd/mm/aaaa, año 1–9999, término igual o posterior al inicio.
El cliente solo ve campañas activas/publicadas con inicio <= hoy <= término,
según la fecha local del teléfono. Se actualizan al volver a la pantalla y al
cambiar el día. No se calculan descuentos ni se alteran precios automáticamente.

El cliente dispone de Colecciones y campañas desde su dashboard, con tarjetas
visuales y acceso al detalle existente de cada producto. El administrador dispone
de Administrar productos y Colecciones y campañas, además de sus funciones anteriores.

Las operaciones administrativas comprueban el usuario de la sesión y su rol
persistido en Room. Las escrituras se realizan dentro de transacciones después
de comprobar ADMINISTRADOR; las consultas administrativas incluyen un filtro SQL
de rol y observación de la sesión. Clientes leen consultas filtradas por publicación.
Las rutas nuevas se añaden a la política central y a las pruebas de acceso sin sesión.

Las referencias opcionales de cotización usan los productos publicados de Room
en la aplicación. Cada solicitud conserva la copia histórica de nombre/id, de modo
que editar o desactivar el producto no reescribe solicitudes anteriores.

## Migraciones y conservación

- AppDatabase pasa de 3 a 5 mediante migraciones explícitas 3→4 y 4→5.
- 3→4 crea productos y carga los ocho diseños iniciales con INSERT OR IGNORE.
- 4→5 añade colecciones, campañas y la tabla de asociaciones con claves foráneas.
- Se conservan 1→2 y 2→3, y se registran todas en MIGRACIONES.
- Se exportan 4.json y 5.json conservando los esquemas históricos.
- Una instalación nueva carga los productos iniciales en onCreate. El repositorio
  también permite inicialización idempotente, con IGNORE para no sobrescribir
  nombres, precios, disponibilidad, imágenes ni productos desactivados.
- FavoritosDatabase mantiene su nombre, versión, entidad y DAO originales.
  Usuarios y solicitudes no cambian de esquema. No hay migración destructiva.

## Archivos creados

Rutas Kotlin relativas a app/src/main/java/com/example/applouaraya_grupogp:

- data/ProductoDao.kt y data/ContenidoDao.kt
- model/ContenidoComercial.kt
- repository/ProductosRepository.kt
- repository/ContenidoRepository.kt
- repository/ImagenesProducto.kt
- validation/ProductoValidator.kt
- validation/ContenidoValidator.kt
- viewmodel/AdminProductosViewModel.kt
- viewmodel/ContenidoViewModels.kt
- ui/screens/AdminProductosScreen.kt
- ui/screens/AdminContenidoScreen.kt
- ui/screens/NovedadesScreen.kt

Otros creados:

- app/schemas/com.example.applouaraya_grupogp.data.AppDatabase/4.json y 5.json
- app/src/test/java/com/example/applouaraya_grupogp/ProductoValidatorTest.kt
- app/src/test/java/com/example/applouaraya_grupogp/ContenidoValidatorTest.kt
- app/src/androidTest/java/com/example/applouaraya_grupogp/ProductosPersistenciaTest.kt
- app/src/androidTest/java/com/example/applouaraya_grupogp/ContenidoPersistenciaTest.kt
- app/src/androidTest/java/com/example/applouaraya_grupogp/ComercialUiTest.kt
- Este informe.

## Archivos modificados

- data/AppDatabase.kt: entidades, DAO, migraciones y semilla.
- model/Producto.kt: entidad Room, publicación e imagen local, conservando campos/IDs.
- model/CotizacionUiState.kt: referencias publicadas reactivas.
- repository/CotizacionRepository.kt: lectura de referencias de Room en la aplicación;
  conserva compatibilidad con los repositorios de prueba anteriores.
- viewmodel/CatalogoViewModel.kt: origen persistente, mismos filtros, carga/error.
- viewmodel/CotizacionViewModel.kt: referencias del catálogo publicado.
- viewmodel/FavoritosViewModel.kt: constructor inyectable para pruebas; misma lógica.
- ui/components/ImagenProducto.kt: soporte de copia de imagen local.
- ui/screens/CatalogoScreen.kt: fuente reactiva para listado, favoritos y detalle;
  inyección aislada para pruebas y apertura del detalle desde colecciones.
- ui/screens/CotizacionScreen.kt: opciones de referencia persistentes.
- ui/screens/DashboardScreen.kt y AdminScreens.kt: accesos nuevos.
- navigation/AppNavigation.kt y PoliticaNavegacion.kt: integración y guardas.
- PoliticaNavegacionTest.kt y NavegacionPerfilTest.kt: incluyen las rutas nuevas.
- AppDatabaseMigrationTest.kt: abre la versión actual usando la cadena completa,
  conservando sus verificaciones históricas 1→2, 2→3 y 1→3.

## Pruebas y aislamiento

Se compiló y ejecutó primero la parte de productos: 35 unitarias aprobadas y
3 instrumentadas de productos aprobadas en el teléfono (8,07 segundos).
Después se implementaron colecciones y campañas y se amplió la verificación.

Resultado final:

- assembleDebug y assembleDebugAndroidTest: aprobados.
- testDebugUnitTest: 39 pruebas, 0 fallos y 0 errores.
- lintDebug: 0 errores, 21 advertencias de versiones, recursos y convenciones.
- Instrumentadas en el teléfono físico R5CY42JVXMT: `OK (20 tests)`, 69,081 segundos.
  Reporte completo: app/build/reports/instrumentadas-etapa4-final.txt.
- git diff --check: sin errores de espacios.
- Comparación de esquemas 3 y 5: usuarios y cotizaciones idénticos.
- No cambia la base, entidad ni DAO de favoritos.

| Grupo instrumentado | Aprobadas |
|---|---:|
| Administración de solicitudes | 2 |
| Migraciones históricas y conservación de credenciales | 3 |
| Interfaz comercial (producto/catálogo y colección/campaña) | 2 |
| Persistencia, permisos, publicación y migraciones de contenido | 3 |
| DAO de cotizaciones | 1 |
| Contexto de la aplicación | 1 |
| Navegación, roles y cierre de sesión | 2 |
| Perfil y preferencias | 3 |
| Productos, favoritos, reapertura y migración 3→4 | 3 |
| Total | 20 |

El primer recorrido final tuvo un fallo en un selector de ComercialUiTest: confundía
el texto conservado en el buscador con la tarjeta del producto ya desactivado.
Se corrigió para excluir campos editables; el recorrido completo repetido aprobó.
No quedan errores detectados pendientes en la compilación y pruebas ejecutadas.
Ambos APK se actualizaron mediante install -r (Success), sin desinstalación.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --no-configuration-cache --max-workers=1
```

Las pruebas instrumentadas nuevas usan Room en memoria o archivos
prueba-productos/prueba-contenido/prueba-migracion con UUID. Los favoritos de prueba
usan una FavoritosDatabase en memoria, inyectada expresamente en la interfaz.
No se usan bases personales, no se llama pm clear ni se desinstala la aplicación.
La limpieza solo cierra o elimina esos archivos exclusivos de prueba.

Las pruebas cubren creación por administrador y lectura cliente, actualización de
precio/descripción/disponibilidad, desactivación conservando fila/favoritos, semilla
repetida sin sobrescritura, reapertura, denegación a cliente/sin sesión, colecciones
publicadas y miembros activos, fechas/publicación de campañas, edición/desactivación
de contenido y migraciones 3→4, 4→5 y 3→5 preservando datos anteriores.

ComercialUiTest crea un producto usando el formulario administrativo, entra como
cliente, lo encuentra en el catálogo, comprueba cambios de precio reactivos, guarda
favoritos y conserva los anteriores. Otro caso abre colección/campaña publicadas
desde el dashboard cliente y reutiliza el detalle del producto.

## Cómo probar en el teléfono

1. Actualizar la app con Run en Android Studio o adb install -r usando la misma
   firma. Ante conflicto de firma, detenerse: no desinstalar ni borrar almacenamiento.
2. Entrar con admin@louaraya.example / LouDemo2026! y abrir Administrar productos.
   Deben aparecer los ocho diseños existentes y los datos creados posteriormente.
3. Crear un producto: nombre, descripción, categoría, material, medidas enteras y
   precio CLP. Publicado está activo por defecto. Seleccionar imagen es opcional.
4. Entrar como cliente, abrir catálogo y buscarlo. Probar los filtros, detalle y
   guardar como favorito. Comprobar que los favoritos anteriores siguen disponibles.
5. Volver al administrador, editar precio, descripción o disponibilidad. Entrar
   como cliente y comprobar los cambios. Desactivar desde administración: debe
   ocultarse, pero seguir listado como desactivado para el administrador. Editar
   y activar Publicado debe recuperarlo, con su favorito anterior intacto.
6. Abrir Colecciones y campañas como administrador. Crear una colección, asociar
   productos y activar Publicada y Activa. Guardar y editar asociaciones.
7. En Campañas, crear título/descripción con fechas que incluyan hoy. Activar
   Publicada y Activa. Probar fechas imposibles o término anterior: deben dar error.
8. Entrar como cliente y abrir Colecciones y campañas. Ver la campaña vigente,
   abrir la colección y sus productos. Borradores, campañas futuras/terminadas y
   productos desactivados no deben mostrarse.
9. Desactivar colección/campaña desde administración y comprobar que se ocultan
   al cliente pero permanecen editables para el administrador.
10. Cerrar y volver a abrir la app, iniciar sesión y comprobar persistencia.
    Revisar además perfil, login/registro, cotizaciones, historial y cambios de estado.

## Límites

Todos los datos e imágenes son locales al teléfono. No hay integración REST ni
sincronización entre dispositivos. Las imágenes no son obligatorias; los ocho
diseños iniciales conservan su marcador si no existe el recurso drawable.
Se conservan las copias locales de imágenes retiradas o de una edición cancelada;
esta etapa no borra esos archivos para evitar eliminar recursos aún referenciados.
No se implementan cámara, QR, mapas, notificaciones ni descuentos automáticos.
La selección real de una fotografía mediante el selector del sistema no se
automatizó en las pruebas: conviene comprobarla con un archivo de prueba en el
teléfono. La compatibilidad declarada mantiene minSdk 26; el recorrido instrumentado
se ejecutó en el teléfono conectado, no en un dispositivo adicional con Android 8.
