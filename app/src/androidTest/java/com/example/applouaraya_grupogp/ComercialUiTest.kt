package com.example.applouaraya_grupogp

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.applouaraya_grupogp.data.*
import com.example.applouaraya_grupogp.navigation.AppNavigation
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.validation.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ComercialUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    private lateinit var db:AppDatabase
    private lateinit var favoritos:FavoritosDatabase
    private lateinit var productos:ProductosRepository
    private var clienteId=0L
    private val archivo="prueba-comercial-ui-${UUID.randomUUID()}"
    private val contexto get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun preparar(){
        db=Room.inMemoryDatabaseBuilder(contexto,AppDatabase::class.java).build()
        favoritos=Room.inMemoryDatabaseBuilder(contexto,FavoritosDatabase::class.java).build()
        runBlocking{
            val usuarios=UsuarioRepository(db.usuarioDao())
            clienteId=usuarios.registrar("Cliente comercial","comercial-ui@example.test","Clave123!").id
            val admin=usuarios.iniciarSesion(UsuarioRepository.CORREO_DEMO,UsuarioRepository.CLAVE_DEMO)!!
            productos=ProductosRepository(db){admin.id};productos.inicializar()
            favoritos.favoritoDao().agregar(FavoritoEntity(clienteId,1))
        }
        val prefs=PreferenciasRepository(contexto.getSharedPreferences(archivo,Context.MODE_PRIVATE))
        compose.setContent{MaterialTheme{AppNavigation(basePrueba=db,preferenciasPrueba=prefs,favoritosPrueba=FavoritosRepository(favoritos.favoritoDao()))}}
    }
    @After fun cerrar(){compose.activityRule.scenario.close();favoritos.close();db.close();contexto.deleteSharedPreferences(archivo)}
    private fun esperar(texto:String){compose.waitUntil(15000){compose.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()}}
    private fun entrar(correo:String,clave:String){
        compose.onNodeWithText("Comenzar").performClick();esperar("Iniciar sesión")
        compose.onNodeWithText("Correo electrónico").performTextInput(correo)
        compose.onNodeWithText("Contraseña").performTextInput(clave)
        compose.onNodeWithText("Ingresar").performClick()
    }
    private fun campo(label:String,valor:String){compose.onNodeWithText(label).performScrollTo().performTextInput(valor)}
    @Test fun crearProductoAdminVerYActualizarCatalogoCliente(){
        entrar(UsuarioRepository.CORREO_DEMO,UsuarioRepository.CLAVE_DEMO);esperar("Administración Lou Araya")
        compose.onNodeWithText("Administrar productos").performScrollTo().performClick();esperar("Crear producto")
        compose.onNodeWithText("Crear producto").performClick()
        campo("Nombre","Producto UI nuevo");campo("Descripción","Descripción artesanal del nuevo diseño")
        compose.onNodeWithText("Interiores").performScrollTo().performClick()
        campo("Material","Algodón");campo("Ancho (cm)","40");campo("Largo / alto (cm)","60");campo("Precio referencial (CLP)","12345")
        compose.onNodeWithText("Guardar producto").performScrollTo().performClick();esperar("Producto guardado")
        compose.onNodeWithText("← Volver").performClick();esperar("Administración Lou Araya")
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick();compose.onNodeWithText("Confirmar").performClick();esperar("Comenzar")
        entrar("comercial-ui@example.test","Clave123!");esperar("¡Bienvenido, Cliente comercial!")
        compose.onNodeWithText("Ver catálogo").performScrollTo().performClick();esperar("Catálogo de macramé")
        compose.onNodeWithText("Buscar por nombre o material").performTextInput("Producto UI nuevo")
        androidx.test.espresso.Espresso.closeSoftKeyboard()
        esperar("Producto UI nuevo");esperar("$12.345 CLP")
        val id=runBlocking{productos.observarPublicados().first().single{it.nombre=="Producto UI nuevo"}.id}
        runBlocking{productos.guardar(ProductoFormulario.desde(db.productoDao().buscar(id)!!).copy(precio="23456",descripcion="Descripción modificada en tiempo real"))}
        esperar("$23.456 CLP")
        compose.onNodeWithText("♡ Guardar").performScrollTo().performClick();esperar("♥ Guardado")
        runBlocking{assertEquals(setOf(1,id),favoritos.favoritoDao().observarIds(clienteId).first().toSet())}
        compose.onNodeWithText("Ver detalle").performScrollTo().performClick();esperar("Descripción modificada en tiempo real")
        runBlocking{productos.desactivar(id)}
        esperar("El producto ya no está disponible.")
        compose.onNodeWithText("Descripción modificada en tiempo real").assertDoesNotExist()
        compose.onNodeWithText("← Catálogo").performClick()
        esperar("0 diseños encontrados")
        // El buscador conserva la consulta; solo debe desaparecer la tarjeta.
        compose.onNode(hasText("Producto UI nuevo") and !hasSetTextAction()).assertDoesNotExist()
        runBlocking{assertNotNull(db.productoDao().buscar(id));assertTrue(favoritos.favoritoDao().existe(clienteId,id))}
    }
    @Test fun coleccionYCampaniaPublicadasVisiblesEnAreaCliente(){
        runBlocking{
            val repo=ContenidoRepository(db,productos)
            repo.guardarColeccion(ColeccionFormulario(nombre="Colección UI natural",descripcion="Diseños para espacios naturales",publicada=true,productos=setOf(1)))
            val formato=DateTimeFormatter.ofPattern("dd/MM/uuuu")
            repo.guardarCampania(CampaniaFormulario(titulo="Campaña UI vigente",descripcion="Conoce la colección de esta temporada",inicio=LocalDate.now().minusDays(1).format(formato),termino=LocalDate.now().plusDays(1).format(formato),publicada=true))
        }
        entrar("comercial-ui@example.test","Clave123!");esperar("¡Bienvenido, Cliente comercial!")
        compose.onNodeWithText("Colecciones y campañas").performScrollTo().performClick()
        esperar("Campaña UI vigente");esperar("Colección UI natural")
        compose.onNodeWithText("Colección UI natural").performScrollTo().performClick();esperar("Tapiz Bohemio")
        compose.onNodeWithText("Tapiz Bohemio").performScrollTo().performClick();esperar("Quitar de favoritos")
        compose.onNodeWithText("Tapiz Bohemio").assertExists()
        runBlocking{assertEquals(listOf(1),favoritos.favoritoDao().observarIds(clienteId).first())}
    }
}
