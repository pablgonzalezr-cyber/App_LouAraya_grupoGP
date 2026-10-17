package com.example.applouaraya_grupogp.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.applouaraya_grupogp.ui.components.ImagenProducto
import com.example.applouaraya_grupogp.validation.ContenidoValidator
import com.example.applouaraya_grupogp.viewmodel.NovedadesViewModel

@Composable
fun NovedadesScreen(vm:NovedadesViewModel,onVolver:()->Unit,onProducto:(Int)->Unit){
    val estado by vm.estado.collectAsState()
    var coleccionId by rememberSaveable{mutableStateOf<Long?>(null)}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle){
        val observer=LifecycleEventObserver{_,e->if(e==Lifecycle.Event.ON_RESUME)vm.cargar()}
        lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}
    }
    BackHandler(coleccionId!=null){coleccionId=null}
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F1E8)).safeDrawingPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        TextButton(onClick={if(coleccionId!=null)coleccionId=null else onVolver()}){Text("← Volver")}
        Text("Colecciones y campañas",style=MaterialTheme.typography.headlineMedium,color=Color(0xFF51362B))
        if(estado.cargando)CircularProgressIndicator()
        estado.error?.let{Text(it,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::cargar){Text("Reintentar")}}
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(coleccionId==null){
                if(!estado.cargando && estado.datos.campanias.isEmpty() && estado.datos.colecciones.isEmpty())item{Text("Pronto tendremos nuevas colecciones y campañas para ti.")}
                items(estado.datos.campanias,key={"campania-${it.id}"}){c->Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFE9D8C6))){Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(c.titulo,style=MaterialTheme.typography.titleLarge,color=Color(0xFF51362B));Text(c.descripcion)
                    Text("${ContenidoValidator.mostrarFecha(c.inicio)} al ${ContenidoValidator.mostrarFecha(c.termino)}",style=MaterialTheme.typography.bodySmall)
                }}}
                items(estado.datos.colecciones,key={"coleccion-${it.coleccion.id}"}){vista->Card(onClick={coleccionId=vista.coleccion.id},colors=CardDefaults.cardColors(containerColor=Color(0xFFFFFBF5))){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    vista.productos.firstOrNull()?.let{ImagenProducto(it,Modifier.fillMaxWidth().height(150.dp))}
                    Text(vista.coleccion.nombre,style=MaterialTheme.typography.titleLarge,color=Color(0xFF51362B));Text(vista.coleccion.descripcion)
                    Text("${vista.productos.size} diseños · Ver colección →")
                }}}
            }else{
                val vista=estado.datos.colecciones.find{it.coleccion.id==coleccionId}
                if(vista==null){if(!estado.cargando)item{Text("Colección no disponible.")}}
                else{
                    item{Text(vista.coleccion.nombre,style=MaterialTheme.typography.titleLarge);Text(vista.coleccion.descripcion)}
                    if(vista.productos.isEmpty())item{Text("Esta colección aún no tiene productos publicados disponibles para mostrar.")}
                    items(vista.productos,key={it.id}){p->Card(onClick={onProducto(p.id)},colors=CardDefaults.cardColors(containerColor=Color(0xFFFFFBF5))){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        ImagenProducto(p,Modifier.fillMaxWidth().height(150.dp));Text(p.nombre,style=MaterialTheme.typography.titleLarge)
                        Text("Precio referencial: ${p.precio} CLP");Text("Ver diseño →")
                    }}}
                }
            }
        }
    }
}
