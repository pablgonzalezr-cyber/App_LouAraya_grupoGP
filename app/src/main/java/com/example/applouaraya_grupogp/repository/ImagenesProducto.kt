package com.example.applouaraya_grupogp.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImagenesProducto {
    fun archivo(context: Context, nombre: String): File? =
        if (Regex("[a-f0-9-]{36}\\.img").matches(nombre)) File(File(context.filesDir, "imagenes_productos"), nombre) else null

    suspend fun importar(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val nombre = "${UUID.randomUUID()}.img"
        val destino = requireNotNull(archivo(context, nombre))
        destino.parentFile?.mkdirs()
        try {
            require(uri.scheme == "content") { "Selecciona una imagen desde el dispositivo" }
            requireNotNull(context.contentResolver.openInputStream(uri)).use { entrada ->
                destino.outputStream().use { salida ->
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val n = entrada.read(buffer)
                        if (n < 0) break
                        total += n
                        require(total <= 10 * 1024 * 1024) { "La imagen debe pesar hasta 10 MB" }
                        salida.write(buffer, 0, n)
                    }
                }
            }
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destino.path, opciones)
            require(opciones.outWidth in 1..16000 && opciones.outHeight in 1..16000) { "Imagen no válida o demasiado grande" }
            nombre
        } catch (e: Exception) { destino.delete(); throw e }
    }
}
