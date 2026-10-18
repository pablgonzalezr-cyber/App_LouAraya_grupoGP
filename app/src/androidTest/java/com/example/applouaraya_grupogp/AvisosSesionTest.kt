package com.example.applouaraya_grupogp

import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.viewmodel.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class AvisosSesionTest {
    @Test fun cambiarUsuarioYLuegoSalirLimpiaEstadoYContador() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        val archivo = "prueba-sesion-avisos-${UUID.randomUUID()}"
        val store = ViewModelStore()
        try {
            val usuarios = UsuarioRepository(db.usuarioDao())
            val cliente = usuarios.registrar("Cliente sesión", "sesionaviso@example.test", "Clave123!")
            val admin = usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO, UsuarioRepository.CLAVE_DEMO)!!
            CotizacionRepository(db.cotizacionDao()).guardar(cliente.id, "sesion-aviso", CotizacionFormulario("Tapiz para prueba de sesión", "Interiores", "Algodón", "40", "60", "1"))
            val sesion = MutableStateFlow(SesionUiState(usuarioId = admin.id, rol = "ADMINISTRADOR"))
            val vm = withContext(Dispatchers.Main) {
                AvisosViewModel(AvisosRepository(db) { sesion.value.usuarioId }, PreferenciasRepository(contexto.getSharedPreferences(archivo, Context.MODE_PRIVATE)), sesion).also { store.put("avisos", it) }
            }
            withTimeout(10000) { vm.estado.first { it.usuarioId == admin.id && !it.cargando && it.noLeidas == 1 } }
            withContext(Dispatchers.Main) { sesion.value = SesionUiState(usuarioId = cliente.id) }
            val nuevo = withTimeout(10000) { vm.estado.first { it.usuarioId == cliente.id && !it.cargando } }
            assertTrue(nuevo.avisos.isEmpty()); assertEquals(0, nuevo.noLeidas)
            withContext(Dispatchers.Main) { sesion.value = SesionUiState() }
            val fuera = withTimeout(10000) { vm.estado.first { it.usuarioId == null } }
            assertTrue(fuera.avisos.isEmpty()); assertEquals(0, fuera.noLeidas)
        } finally { withContext(Dispatchers.Main) { store.clear() }; db.close(); contexto.deleteSharedPreferences(archivo) }
    }
}
