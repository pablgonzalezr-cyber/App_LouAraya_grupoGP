package com.example.applouaraya_grupogp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.applouaraya_grupogp.model.*
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdminContenidoState(val datos: ContenidoAdminDatos = ContenidoAdminDatos(emptyList(),emptyList(),emptyList(),emptyList()),
    val campanias: Boolean = false, val editando: Boolean = false,
    val coleccion: ColeccionFormulario = ColeccionFormulario(), val campania: CampaniaFormulario = CampaniaFormulario(),
    val errores: Map<String,String> = emptyMap(), val cargando: Boolean = true, val guardando: Boolean = false,
    val error: String? = null, val mensaje: String? = null)

class AdminContenidoViewModel(private val repo: ContenidoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AdminContenidoState())
    val estado = _estado.asStateFlow()
    private var job: Job? = null
    init { cargar() }
    fun cargar() {
        job?.cancel(); _estado.value=_estado.value.copy(cargando=true,error=null)
        job=viewModelScope.launch {
            try { repo.observarAdministracion().collect { _estado.value=_estado.value.copy(datos=it,cargando=false) } }
            catch(e:CancellationException){throw e}
            catch(e:Exception){_estado.value=_estado.value.copy(cargando=false,error=e.message,datos=ContenidoAdminDatos(emptyList(),emptyList(),emptyList(),emptyList()))}
        }
    }
    fun seccion(campanias: Boolean) { if(!_estado.value.guardando) _estado.value=_estado.value.copy(campanias=campanias,editando=false,errores=emptyMap(),error=null,mensaje=null) }
    fun nueva() { if(!_estado.value.guardando) _estado.value=_estado.value.copy(editando=true,coleccion=ColeccionFormulario(),campania=CampaniaFormulario(),errores=emptyMap(),error=null,mensaje=null) }
    fun editar(c: ColeccionEntity) {
        _estado.value=_estado.value.copy(editando=true,campanias=false,errores=emptyMap(),error=null,mensaje=null,
            coleccion=ColeccionFormulario(c.id,c.nombre,c.descripcion,c.activa,c.publicada,_estado.value.datos.miembros.filter { it.coleccionId==c.id }.map { it.productoId }.toSet()))
    }
    fun editar(c: CampaniaEntity) {
        _estado.value=_estado.value.copy(editando=true,campanias=true,errores=emptyMap(),error=null,mensaje=null,
            campania=CampaniaFormulario(c.id,c.titulo,c.descripcion,ContenidoValidator.mostrarFecha(c.inicio),ContenidoValidator.mostrarFecha(c.termino),c.publicada,c.activa))
    }
    fun cambiar(f: ColeccionFormulario) { if(!_estado.value.guardando) _estado.value=_estado.value.copy(coleccion=f,errores=emptyMap(),error=null) }
    fun cambiar(f: CampaniaFormulario) { if(!_estado.value.guardando) _estado.value=_estado.value.copy(campania=f,errores=emptyMap(),error=null) }
    fun cancelar() { if(!_estado.value.guardando) _estado.value=_estado.value.copy(editando=false,error=null) }
    private fun ejecutar(accion:suspend()->Unit) {
        if(_estado.value.guardando)return
        _estado.value=_estado.value.copy(guardando=true,error=null,mensaje=null)
        viewModelScope.launch {
            try { accion(); _estado.value=_estado.value.copy(guardando=false,mensaje="Cambios guardados") }
            catch(e:CancellationException){throw e}
            catch(e:Exception){_estado.value=_estado.value.copy(guardando=false,error=e.message?:"No se pudo guardar")}
        }
    }
    fun guardar() {
        val actual=_estado.value
        val errores=if(actual.campanias) ContenidoValidator.campania(actual.campania) else ContenidoValidator.coleccion(actual.coleccion)
        if(errores.isNotEmpty()){_estado.value=actual.copy(errores=errores);return}
        ejecutar {
            if(actual.campanias) repo.guardarCampania(actual.campania) else repo.guardarColeccion(actual.coleccion)
            _estado.value=_estado.value.copy(editando=false)
        }
    }
    fun desactivar(id:Long,campania:Boolean)=ejecutar { if(campania) repo.desactivarCampania(id) else repo.desactivarColeccion(id) }
}

data class NovedadesState(val datos: ContenidoClienteDatos = ContenidoClienteDatos(emptyList(),emptyList()), val cargando:Boolean=true,val error:String?=null)
class NovedadesViewModel(private val repo:ContenidoRepository):ViewModel(){
    private val _estado=MutableStateFlow(NovedadesState())
    val estado=_estado.asStateFlow()
    private var job:Job?=null
    private var fecha=LocalDate.now()
    init {
        cargar()
        viewModelScope.launch { while(isActive){delay(60_000);if(fecha!=LocalDate.now())cargar()} }
    }
    fun cargar(){
        job?.cancel(); fecha=LocalDate.now();_estado.value=_estado.value.copy(cargando=true,error=null)
        job=viewModelScope.launch {
            try{repo.observarCliente(fecha).collect{_estado.value=NovedadesState(it,false)}}
            catch(e:CancellationException){throw e}
            catch(_:Exception){_estado.value=NovedadesState(cargando=false,error="No se pudieron cargar colecciones y campañas")}
        }
    }
}
