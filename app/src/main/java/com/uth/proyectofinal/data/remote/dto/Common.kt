package com.uth.proyectofinal.data.remote.dto

data class Ref(val id: Int = 0, val nombre: String = "", val telefono: String? = null)

data class MessageDto(val message: String? = null)

data class PruebaPushDto(val message: String? = null, val dispositivos: Int = 0, val enviados: Int = 0)

object Rol {
    const val DUENO = "DUENO"
    const val VETERINARIO = "VETERINARIO"
    const val ADMINISTRADOR = "ADMINISTRADOR"
}
