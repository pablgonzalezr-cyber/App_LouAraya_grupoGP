package com.example.applouaraya_grupogp

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.CotizacionFormulario
import com.example.applouaraya_grupogp.repository.UsuarioRepository
import com.example.applouaraya_grupogp.repository.CotizacionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun migrarConservaCuentaPermiteLoginYReabrirSolicitud() = runBlocking {
        // Base exclusiva de prueba; nunca se abre ni borra lou_araya.db.
        val nombre = "prueba-migracion-${UUID.randomUUID()}.db"
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val password = "ClaveFicticia123"
        val salt = ByteArray(16) { it.toByte() }
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        val hash = try { javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded } finally { spec.clearPassword() }
        val saltTexto = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
        val hashTexto = android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
        try {
            helper.createDatabase(nombre, 1).use { anterior ->
                anterior.execSQL("INSERT INTO usuarios (id,nombre,correo,salt,claveHash) VALUES (?,?,?,?,?)", arrayOf(7, "Cliente anterior", "anterior@example.test", saltTexto, hashTexto))
            }
            helper.runMigrationsAndValidate(nombre, 2, true, AppDatabase.MIGRATION_1_2).use { migrada ->
                migrada.query("SELECT nombre,salt,claveHash FROM usuarios WHERE id = 7").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("Cliente anterior", cursor.getString(0))
                    assertEquals(saltTexto, cursor.getString(1))
                    assertEquals(hashTexto, cursor.getString(2))
                }
            }
            val db = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).addMigrations(AppDatabase.MIGRATION_1_2).build()
            val id = try {
                val usuario = UsuarioRepository(db.usuarioDao()).iniciarSesion("anterior@example.test", password)
                assertNotNull(usuario)
                assertEquals(7L, usuario!!.id)
                CotizacionRepository(db.cotizacionDao()).guardar(usuario.id, "token-migracion", CotizacionFormulario("Tapiz para habitación", "Interiores", "Algodón", "45", "70", "1"))
            } finally { db.close() }
            val reabierta = Room.databaseBuilder(contexto, AppDatabase::class.java, nombre).addMigrations(AppDatabase.MIGRATION_1_2).build()
            try {
                assertEquals("PENDIENTE", reabierta.cotizacionDao().observarDetalle(7, id).first()!!.estado)
                assertEquals(1, reabierta.cotizacionDao().observarPropias(7).first().size)
                assertNotNull(UsuarioRepository(reabierta.usuarioDao()).iniciarSesion("anterior@example.test", password))
            } finally { reabierta.close() }
        } finally { contexto.deleteDatabase(nombre) }
    }
}
