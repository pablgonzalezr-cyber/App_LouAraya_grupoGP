package com.example.applouaraya_grupogp

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.repository.UsuarioRepository
import com.example.applouaraya_grupogp.viewmodel.SesionViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class RegistroConfirmacionTest {
    @Test fun confirmacionesRapidasYPosterioresAlExitoGuardanUnaSolaCuenta() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(contexto, AppDatabase::class.java).build()
        val store = ViewModelStore()
        try {
            val vm = withContext(Dispatchers.Main) {
                SesionViewModel(contexto.applicationContext as Application, UsuarioRepository(db.usuarioDao())).also {
                    store.put("sesion-registro", it)
                    repeat(10) { _ -> it.registrar("Cliente único", "unico@example.test", "Clave123!") }
                    assertTrue(it.estado.value.cargando)
                }
            }
            val registrado = withTimeout(15000) { vm.estado.first { it.usuarioId != null } }
            withContext(Dispatchers.Main) {
                repeat(5) { vm.registrar("Segundo intento", "otro@example.test", "OtraClave123!") }
            }
            assertEquals(registrado.usuarioId, vm.estado.value.usuarioId)
            assertNotNull(db.usuarioDao().buscarPorCorreo("unico@example.test"))
            assertNull(db.usuarioDao().buscarPorCorreo("otro@example.test"))
            db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM usuarios WHERE correo='unico@example.test'").use { c ->
                assertTrue(c.moveToFirst()); assertEquals(1, c.getInt(0))
            }
        } finally { withContext(Dispatchers.Main) { store.clear() }; db.close() }
    }
}
