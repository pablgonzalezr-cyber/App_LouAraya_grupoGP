package com.example.applouaraya_grupogp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.applouaraya_grupogp.ui.design.*
import com.example.applouaraya_grupogp.ui.components.ImagenProducto
import com.example.applouaraya_grupogp.model.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.viewmodel.AdminContenidoViewModel

@Composable
fun AdminContenidoScreen(vm:AdminContenidoViewModel,onVolver:()->Unit){
    val estado by vm.estado.collectAsState()
    var desactivar by remember { mutableStateOf<Pair<Long,Boolean>?>(null) }
    BackHandler(estado.editando){if(!estado.guardando)vm.cancelar()}
    AdminPage(if(estado.campanias) "Administrar campañas" else "Administrar colecciones",
        { if(estado.editando) vm.cancelar() else onVolver() }, scroll = estado.editando || (vm.usaServidor && !estado.conectado), habilitado = !estado.guardando) {
        estado.error?.let{Text(it,color=MaterialTheme.colorScheme.error);if(!estado.editando)TextButton(onClick=vm::cargar){Text("Reintentar")}}
        estado.mensaje?.let{Text(it,color=Color(0xFF38684E))}
        if(estado.cargando)CircularProgressIndicator()
        if (vm.usaServidor && !estado.conectado) {
            if (vm.permiteAdministracion) AccesoCatalogo(estado.cargando, vm::conectar)
            else Text("Configura la URL HTTPS de administración para conectar al servidor.")
        } else if(estado.editando){
            val habilitado=!estado.guardando
            LouPanel(Modifier.fillMaxWidth()){
                if(estado.campanias){
                    val f=estado.campania
                    CampoContenido("Título",f.titulo,estado.errores["titulo"],habilitado){vm.cambiar(f.copy(titulo=it))}
                    CampoContenido("Descripción",f.descripcion,estado.errores["descripcion"],habilitado,multilinea=true){vm.cambiar(f.copy(descripcion=it))}
                    CampoContenido("Inicio (dd/mm/aaaa)",f.inicio,estado.errores["inicio"],habilitado){vm.cambiar(f.copy(inicio=it))}
                    CampoContenido("Término (dd/mm/aaaa)",f.termino,estado.errores["termino"],habilitado){vm.cambiar(f.copy(termino=it))}
                    OpcionContenido("Publicada",f.publicada,habilitado){vm.cambiar(f.copy(publicada=it))}
                    OpcionContenido("Activa",f.activa,habilitado){vm.cambiar(f.copy(activa=it))}
                    Text("Solo será visible durante sus fechas, estando activa y publicada. No aplica descuentos automáticos.",style=MaterialTheme.typography.bodySmall)
                }else{
                    val f=estado.coleccion
                    CampoContenido("Nombre",f.nombre,estado.errores["nombre"],habilitado){vm.cambiar(f.copy(nombre=it))}
                    CampoContenido("Descripción",f.descripcion,estado.errores["descripcion"],habilitado,multilinea=true){vm.cambiar(f.copy(descripcion=it))}
                    OpcionContenido("Publicada",f.publicada,habilitado){vm.cambiar(f.copy(publicada=it))}
                    OpcionContenido("Activa",f.activa,habilitado){vm.cambiar(f.copy(activa=it))}
                    Text("Productos de la colección",style=MaterialTheme.typography.titleMedium)
                    Text("Los productos desactivados se conservan asociados, pero no se muestran al cliente.",style=MaterialTheme.typography.bodySmall)
                    estado.errores["productos"]?.let{Text(it,color=MaterialTheme.colorScheme.error)}
                    val ausentes = f.productos - estado.datos.productos.map { it.id }.toSet()
                    ausentes.forEach { id ->
                        TextButton(onClick = { vm.cambiar(f.copy(productos = f.productos - id)) }, enabled = habilitado) {
                            Text("Quitar producto archivado #$id")
                        }
                    }
                    estado.datos.productos.forEach{p->
                        Row(Modifier.fillMaxWidth().testTag("coleccion-producto-${p.id}")){
                            Checkbox(checked=p.id in f.productos,enabled=habilitado,onCheckedChange={seleccionado->vm.cambiar(f.copy(productos=if(seleccionado)f.productos+p.id else f.productos-p.id))})
                            Text("#${p.id} ${p.nombre}${if(p.activo)"" else " (desactivado)"}",modifier=Modifier.weight(1f))
                        }
                    }
                }
                LouPrimaryButton(if(estado.guardando) "Guardando…" else "Guardar cambios", vm::guardar, Modifier.fillMaxWidth(), habilitado)
            }
        }else{
            val disponible = !estado.guardando && !estado.cargando
            if (vm.usaServidor) Row {
                TextButton(onClick = vm::cargar, enabled = disponible) { Text("Actualizar") }
                TextButton(onClick = vm::desconectar, enabled = disponible) { Text("Cambiar cuenta") }
            }
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(selected=!estado.campanias,onClick={vm.seccion(false)},enabled=!estado.guardando,label={Text("Colecciones")})
                FilterChip(selected=estado.campanias,onClick={vm.seccion(true)},enabled=!estado.guardando,label={Text("Campañas")})
            }
            LouPrimaryButton(if(estado.campanias) "Crear campaña" else "Crear colección", vm::nueva, enabled = disponible)
            if (estado.campanias) {
                if(estado.datos.campanias.isEmpty()) Text("No hay campañas todavía.")
                AdminGrid(estado.datos.campanias, { it.id }, Modifier.weight(1f)) { c ->
                    AdminCampaniaCard(c, disponible, { vm.editar(c) }, { desactivar = c.id to true },
                        if (vm.usaServidor) ({ vm.publicar(c.id, true, !c.publicada) }) else null)
                }
            } else {
                if(estado.datos.colecciones.isEmpty()) Text("No hay colecciones todavía.")
                AdminGrid(estado.datos.colecciones, { it.id }, Modifier.weight(1f)) { c ->
                    val ids = estado.datos.miembros.filter { it.coleccionId == c.id }.map { it.productoId }.toSet()
                    AdminColeccionCard(c, estado.datos.productos.filter { it.id in ids }, ids.size, disponible,
                        { vm.editar(c) }, { desactivar = c.id to false },
                        if (vm.usaServidor) ({ vm.publicar(c.id, false, !c.publicada) }) else null)
                }
            }
        }
    }
    desactivar?.let{(id,campania)->AlertDialog(onDismissRequest={desactivar=null},title={Text("Confirmar desactivación")},text={Text("Se ocultará al cliente y se conservará el registro.")},
        confirmButton={TextButton(onClick={desactivar=null;vm.desactivar(id,campania)}){Text("Confirmar")}},dismissButton={TextButton(onClick={desactivar=null}){Text("Cancelar")}})}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminColeccionCard(c: ColeccionEntity, productos: List<Producto>, total: Int,
    habilitado: Boolean, editar: () -> Unit, desactivar: () -> Unit, publicar: (() -> Unit)? = null) {
    LouPanel(Modifier.fillMaxWidth()) {
        productos.firstOrNull { it.imagenNombre.isNotBlank() || it.imagenArchivo.isNotBlank() }?.let {
            ImagenProducto(it, Modifier.fillMaxWidth().aspectRatio(1f))
        }
        LouEyebrow("Colección")
        Text(c.nombre, style = MaterialTheme.typography.titleLarge, color = LouColors.Cafe)
        Text(c.descripcion, color = LouColors.TextoSecundario)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminStatus(if(c.publicada) "Publicada" else "Borrador")
            AdminStatus(if(c.activa) "Activa" else "Desactivada")
        }
        Text("$total productos asociados", color = LouColors.TextoSecundario)
        AdminContenidoAcciones(habilitado, c.activa, editar, desactivar, c.publicada, publicar)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCampaniaCard(c: CampaniaEntity, habilitado: Boolean, editar: () -> Unit, desactivar: () -> Unit, publicar: (() -> Unit)? = null) {
    LouPanel(Modifier.fillMaxWidth()) {
        LouEyebrow("Campaña")
        Text(c.titulo, style = MaterialTheme.typography.titleLarge, color = LouColors.Cafe)
        Text(c.descripcion, color = LouColors.TextoSecundario)
        Surface(color = LouColors.ArenaClara, shape = LouShape) {
            Text("${com.example.applouaraya_grupogp.validation.ContenidoValidator.mostrarFecha(c.inicio)} a ${com.example.applouaraya_grupogp.validation.ContenidoValidator.mostrarFecha(c.termino)}",
                Modifier.padding(12.dp), color = LouColors.Cafe)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdminStatus(if(c.publicada) "Publicada" else "Borrador")
            AdminStatus(if(c.activa) "Activa" else "Desactivada")
            if(c.activa && c.publicada) {
                val periodo = runCatching {
                    val hoy = java.time.LocalDate.now(java.time.ZoneId.of("America/Santiago"))
                    when {
                        hoy < java.time.LocalDate.parse(c.inicio) -> "Programada"
                        hoy > java.time.LocalDate.parse(c.termino) -> "Finalizada"
                        else -> "Vigente"
                    }
                }.getOrNull()
                periodo?.let { AdminStatus(it) }
            }
        }
        AdminContenidoAcciones(habilitado, c.activa, editar, desactivar, c.publicada, publicar)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AdminContenidoAcciones(habilitado: Boolean, activa: Boolean, editar: () -> Unit, desactivar: () -> Unit,
                                  publicada: Boolean, publicar: (() -> Unit)?) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = editar, enabled = habilitado) { Text("Editar") }
        if (publicar != null) TextButton(onClick = publicar, enabled = habilitado) { Text(if (publicada) "Ocultar" else "Publicar") }
        if(activa) TextButton(onClick = desactivar, enabled = habilitado) { Text("Desactivar") }
    }
}
