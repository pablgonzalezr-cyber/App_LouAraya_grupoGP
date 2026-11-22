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
    val error: String? = null, val mensaje: String? = null, val conectado: Boolean = false)

class AdminContenidoViewModel(private val repo: ContenidoRepository) : ViewModel() {
    private val _estado = MutableStateFlow(AdminContenidoState())
    val estado = _estado.asStateFlow()
    private var job: Job? = null
    val usaServidor = repo.remoto != null
    val permiteAdministracion = repo.permiteAdministracion
    private var credencial: String? = null

    init { if (usaServidor) _estado.value = AdminContenidoState(cargando = false) else cargar() }

    fun conectar(usuario: String, clave: String) {
        credencial = ProductosRemotos.credencial(usuario, clave)
        cargar()
    }

    fun desconectar() {
        job?.cancel()
        credencial = null
        _estado.value = AdminContenidoState(cargando = false)
    }

    private suspend fun actualizarRemoto() {
        val datos = repo.administracionRemota(checkNotNull(credencial))
        _estado.value = _estado.value.copy(datos = datos, conectado = true, cargando = false)
    }
    fun cargar() {
        if (_estado.value.guardando || (usaServidor && credencial == null)) return
        job?.cancel(); _estado.value=_estado.value.copy(cargando=true,error=null)
        job=viewModelScope.launch {
            try { if (usaServidor) actualizarRemoto() else repo.observarAdministracion().collect { _estado.value=_estado.value.copy(datos=it,cargando=false) } }
            catch(e:CancellationException){throw e}
            catch(e:Exception){_estado.value=_estado.value.copy(cargando=false,conectado=false,error=e.message,datos=ContenidoAdminDatos(emptyList(),emptyList(),emptyList(),emptyList()))}
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
    private fun ejecutar(accion: suspend () -> CampaniaEntity?) {
        if(_estado.value.guardando || _estado.value.cargando)return
        _estado.value=_estado.value.copy(guardando=true,error=null,mensaje=null)
        viewModelScope.launch {
            try {
                if (usaServidor) repo.exigirAdministrador()
                val campaniaGuardada = accion()
                _estado.value = _estado.value.copy(editando = false, mensaje = "Cambios guardados")
                if (usaServidor) {
                    actualizarRemoto()
                    campaniaGuardada?.let { repo.avisarPublicacion(it) }
                }
                _estado.value = _estado.value.copy(guardando = false)
            }
            catch(e:CancellationException){throw e}
            catch (e: Exception) {
                val confirmado = _estado.value.mensaje != null
                val error = if (confirmado) "El servidor guardó el cambio. Error al actualizar los datos locales: ${e.message}"
                    else e.message ?: "No se pudo guardar"
                _estado.value = _estado.value.copy(guardando = false, error = error)
            }
        }
    }
    fun guardar() {
        val actual=_estado.value
        val errores=if(actual.campanias) ContenidoValidator.campania(actual.campania) else ContenidoValidator.coleccion(actual.coleccion)
        if(errores.isNotEmpty()){_estado.value=actual.copy(errores=errores);return}
        ejecutar {
            val remoto = repo.remoto
            if (remoto != null) {
                val cuenta = checkNotNull(credencial)
                if (actual.campanias) remoto.guardar(cuenta, actual.campania)
                else { remoto.guardar(cuenta, actual.coleccion); null }
            } else {
                if(actual.campanias) repo.guardarCampania(actual.campania) else repo.guardarColeccion(actual.coleccion)
                null
            }
        }
    }
    fun desactivar(id:Long,campania:Boolean)=ejecutar {
        val remoto = repo.remoto
        if (remoto != null) remoto.desactivar(checkNotNull(credencial), id, campania)
        else if(campania) repo.desactivarCampania(id) else repo.desactivarColeccion(id)
        null
    }

    fun publicar(id: Long, campania: Boolean, publicada: Boolean) = ejecutar {
        checkNotNull(repo.remoto).publicar(checkNotNull(credencial), id, campania, publicada)
    }
}

data class NovedadesState(val datos: ContenidoClienteDatos = ContenidoClienteDatos(emptyList(),emptyList()), val cargando:Boolean=true,val error:String?=null)
class NovedadesViewModel(private val repo:ContenidoRepository):ViewModel(){
    private val _estado=MutableStateFlow(NovedadesState())
    val estado=_estado.asStateFlow()
    private var job:Job?=null
    private fun hoy() = if (repo.remoto == null) LocalDate.now() else LocalDate.now(java.time.ZoneId.of("America/Santiago"))
    private var fecha=hoy()
    init {
        cargar()
        viewModelScope.launch { while(isActive){delay(60_000);if(fecha!=hoy())cargar()} }
    }
    fun cargar(){
        job?.cancel(); fecha=hoy();_estado.value=_estado.value.copy(cargando=true,error=null)
        job=viewModelScope.launch {
            try{repo.observarCliente(fecha).collect{_estado.value=NovedadesState(it,false)}}
            catch(e:CancellationException){throw e}
            catch(e:Exception){_estado.value=NovedadesState(cargando=false,error=e.message ?: "No se pudieron cargar colecciones y campañas")}
        }
    }
}
