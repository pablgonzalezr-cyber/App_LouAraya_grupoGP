package com.example.applouaraya_grupogp.model

enum class RolUsuario { CLIENTE, ADMINISTRADOR;
    companion object {
        fun desde(valor: String?): RolUsuario = entries.find { it.name == valor } ?: CLIENTE
    }
}
