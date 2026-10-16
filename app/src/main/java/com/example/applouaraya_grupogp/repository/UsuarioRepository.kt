package com.example.applouaraya_grupogp.repository

import android.util.Base64
import com.example.applouaraya_grupogp.data.UsuarioDao
import com.example.applouaraya_grupogp.model.UsuarioEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class UsuarioRepository(
    private val dao: UsuarioDao
) {

    suspend fun registrar(
        nombre: String,
        correo: String,
        clave: String
    ): UsuarioEntity = withContext(Dispatchers.IO) {

        val correoNormalizado =
            correo.trim().lowercase(Locale.ROOT)

        if (dao.buscarPorCorreo(correoNormalizado) != null) {
            throw IllegalArgumentException(
                "Este correo ya está registrado"
            )
        }

        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)

        val salt = Base64.encodeToString(
            saltBytes,
            Base64.NO_WRAP
        )

        val usuario = UsuarioEntity(
            nombre = nombre.trim(),
            correo = correoNormalizado,
            salt = salt,
            claveHash = generarHash(clave, saltBytes)
        )

        val id = dao.insertar(usuario)
        usuario.copy(id = id)
    }

    suspend fun iniciarSesion(
        correo: String,
        clave: String
    ): UsuarioEntity? = withContext(Dispatchers.IO) {

        val correoNormalizado =
            correo.trim().lowercase(Locale.ROOT)

        val usuario = dao.buscarPorCorreo(correoNormalizado)
            ?: return@withContext null

        val saltBytes = Base64.decode(
            usuario.salt,
            Base64.NO_WRAP
        )

        val hashIngresado = generarHash(clave, saltBytes)

        val coinciden = MessageDigest.isEqual(
            hashIngresado.toByteArray(Charsets.UTF_8),
            usuario.claveHash.toByteArray(Charsets.UTF_8)
        )

        if (coinciden) usuario else null
    }

    private fun generarHash(
        clave: String,
        salt: ByteArray
    ): String {
        val especificacion = PBEKeySpec(
            clave.toCharArray(),
            salt,
            120_000,
            256
        )

        return try {
            val resultado = SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(especificacion)
                .encoded

            Base64.encodeToString(
                resultado,
                Base64.NO_WRAP
            )
        } finally {
            especificacion.clearPassword()
        }
    }
}

