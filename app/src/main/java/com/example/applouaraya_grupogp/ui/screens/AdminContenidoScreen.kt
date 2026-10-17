package com.example.applouaraya_grupogp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.applouaraya_grupogp.viewmodel.AdminContenidoViewModel

@Composable
fun AdminContenidoScreen(vm:AdminContenidoViewModel,onVolver:()->Unit){
    val estado by vm.estado.collectAsState()
    var desactivar by remember { mutableStateOf<Pair<Long,Boolean>?>(null) }
    BackHandler(estado.editando){if(!estado.guardando)vm.cancelar()}
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding().imePadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        TextButton(onClick={if(estado.editando)vm.cancelar() else onVolver()},enabled=!estado.guardando){Text("← Volver")}
        Text(if(estado.campanias)"Administrar campañas" else "Administrar colecciones",style=MaterialTheme.typography.headlineMedium,color=Color(0xFF51362B))
        estado.error?.let{Text(it,color=MaterialTheme.colorScheme.error);if(!estado.editando)TextButton(onClick=vm::cargar){Text("Reintentar")}}
        estado.mensaje?.let{Text(it,color=Color(0xFF38684E))}
        if(estado.cargando)CircularProgressIndicator()
        if(estado.editando){
            val habilitado=!estado.guardando
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
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
                    estado.datos.productos.forEach{p->
                        Row(Modifier.fillMaxWidth()){
                            Checkbox(checked=p.id in f.productos,enabled=habilitado,onCheckedChange={seleccionado->vm.cambiar(f.copy(productos=if(seleccionado)f.productos+p.id else f.productos-p.id))})
                            Text("#${p.id} ${p.nombre}${if(p.activo)"" else " (desactivado)"}",modifier=Modifier.weight(1f))
                        }
                    }
                }
                Button(onClick=vm::guardar,enabled=habilitado,colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFA65E43))){Text(if(estado.guardando)"Guardando…" else "Guardar cambios")}
            }
        }else{
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(selected=!estado.campanias,onClick={vm.seccion(false)},enabled=!estado.guardando,label={Text("Colecciones")})
                FilterChip(selected=estado.campanias,onClick={vm.seccion(true)},enabled=!estado.guardando,label={Text("Campañas")})
            }
            Button(onClick=vm::nueva,enabled=!estado.guardando){Text(if(estado.campanias)"Crear campaña" else "Crear colección")}
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)){
                if(estado.campanias){
                    if(estado.datos.campanias.isEmpty())item{Text("No hay campañas todavía.")}
                    items(estado.datos.campanias,key={it.id}){c->Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFFBF5))){Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Text(c.titulo,style=MaterialTheme.typography.titleMedium)
                        Text("${c.inicio} a ${c.termino} · ${if(c.publicada)"Publicada" else "Borrador"} · ${if(c.activa)"Activa" else "Desactivada"}")
                        Row{TextButton(onClick={vm.editar(c)},enabled=!estado.guardando){Text("Editar")};if(c.activa)TextButton(onClick={desactivar=c.id to true},enabled=!estado.guardando){Text("Desactivar")}}
                    }}}
                }else{
                    if(estado.datos.colecciones.isEmpty())item{Text("No hay colecciones todavía.")}
                    items(estado.datos.colecciones,key={it.id}){c->Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFFBF5))){Column(Modifier.fillMaxWidth().padding(14.dp)){
                        Text(c.nombre,style=MaterialTheme.typography.titleMedium)
                        Text("${if(c.publicada)"Publicada" else "Borrador"} · ${if(c.activa)"Activa" else "Desactivada"}")
                        Text("${estado.datos.miembros.count{it.coleccionId==c.id}} productos asociados")
                        Row{TextButton(onClick={vm.editar(c)},enabled=!estado.guardando){Text("Editar")};if(c.activa)TextButton(onClick={desactivar=c.id to false},enabled=!estado.guardando){Text("Desactivar")}}
                    }}}
                }
            }
        }
    }
    desactivar?.let{(id,campania)->AlertDialog(onDismissRequest={desactivar=null},title={Text("Confirmar desactivación")},text={Text("Se ocultará al cliente y se conservará el registro.")},
        confirmButton={TextButton(onClick={desactivar=null;vm.desactivar(id,campania)}){Text("Confirmar")}},dismissButton={TextButton(onClick={desactivar=null}){Text("Cancelar")}})}
}
