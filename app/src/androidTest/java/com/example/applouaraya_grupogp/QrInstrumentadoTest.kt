package com.example.applouaraya_grupogp

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.ViewModelStore
import com.example.applouaraya_grupogp.data.AppDatabase
import com.example.applouaraya_grupogp.model.Producto
import com.example.applouaraya_grupogp.repository.*
import com.example.applouaraya_grupogp.viewmodel.QrViewModel
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class QrInstrumentadoTest {
    @Test fun consultaRoomDistingueValidoInvalidoInexistenteYDesactivado() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            db.productoDao().insertar(Producto(42,"Producto QR nuevo","Interiores","Algodón",10000,"Producto de prueba QR",40,60))
            db.productoDao().insertar(Producto(43,"Producto oculto","Interiores","Algodón",10000,"Producto de prueba QR",40,60,activo=false))
            val repo = QrRepository(db.productoDao())
            assertEquals(ResultadoQr.Encontrado(42), repo.resolver("LOUARAYA:PRODUCTO:42"))
            assertTrue((repo.resolver("LOUARAYA:PRODUCTO:43") as ResultadoQr.Error).mensaje.contains("desactivado"))
            assertTrue((repo.resolver("LOUARAYA:PRODUCTO:999") as ResultadoQr.Error).mensaje.contains("no existe"))
            assertTrue((repo.resolver("OTRO:42") as ResultadoQr.Error).mensaje.contains("inválido"))
        } finally { db.close() }
    }
    @Test fun qrGeneradoSeLeeConElDetectorRealMlKit() {
        val codigo = ProductoQr.codigoPara(42)
        val imagen = QrImagen.generar(codigo)
        val detector = QrImagen.detector()
        try {
            val encontrados = Tasks.await(detector.process(InputImage.fromBitmap(imagen,0)),20,TimeUnit.SECONDS)
            assertEquals(codigo, encontrados.single().rawValue)
        } finally { detector.close(); imagen.recycle() }
    }
    @Test fun lecturasDuplicadasSeBloqueanHastaReintentar() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext,AppDatabase::class.java).build()
        val store = ViewModelStore()
        try {
            db.productoDao().insertar(Producto(42,"Producto QR","Interiores","Algodón",10000,"Producto QR de prueba",40,60))
            val vm = withContext(Dispatchers.Main) {
                QrViewModel(QrRepository(db.productoDao())).also {
                    store.put("qr",it); it.leer("invalido"); it.leer(ProductoQr.codigoPara(42))
                }
            }
            assertTrue(vm.estado.value.bloqueado); assertNotNull(vm.estado.value.error); assertNull(vm.estado.value.productoId)
            withContext(Dispatchers.Main) { vm.reintentar(); vm.leer(ProductoQr.codigoPara(42)); vm.leer("otro") }
            val resultado = withTimeout(10000) { vm.estado.first { it.productoId != null } }
            assertEquals(42,resultado.productoId); assertNull(resultado.error)
        } finally { withContext(Dispatchers.Main) { store.clear() }; db.close() }
    }
}
