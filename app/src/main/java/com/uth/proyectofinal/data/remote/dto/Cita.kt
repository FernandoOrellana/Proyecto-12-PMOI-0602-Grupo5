package com.uth.proyectofinal.data.remote.dto

data class Cita(
    val id: Int = 0,
    val fecha: String = "",
    val hora: String = "",
    val estado: String = "",
    val mascota: Ref = Ref(),
    val servicio: ServicioResumen = ServicioResumen(),
    val veterinario: Ref = Ref(),
    val dueno: Ref? = null,
    val consulta: Consulta? = null,
    val cobrada: Boolean = false,
)

data class ServicioResumen(val id: Int = 0, val nombre: String = "", val precio: Double = 0.0)

data class CitaRequest(
    val mascotaId: Int,
    val servicioId: Int,
    val veterinarioId: Int,
    val fecha: String,
    val hora: String,
)

data class Consulta(
    val id: Int = 0,
    val citaId: Int = 0,
    val fecha: String = "",
    val diagnostico: String = "",
    val tratamiento: String? = null,
    val observaciones: String? = null,
    val peso: Double? = null,
    val veterinario: Ref = Ref(),
)

data class ConsultaRequest(
    val diagnostico: String,
    val tratamiento: String? = null,
    val observaciones: String? = null,
    val peso: Double? = null,
)

data class ObservacionesRequest(
    val observaciones: String,
)

object CitaEstado {
    const val PENDIENTE = "PENDIENTE"
    const val CONFIRMADA = "CONFIRMADA"
    const val EN_CONSULTA = "EN_CONSULTA"
    const val ATENDIDA = "ATENDIDA"
    const val CANCELADA = "CANCELADA"
}
