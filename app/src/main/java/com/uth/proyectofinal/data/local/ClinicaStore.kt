package com.uth.proyectofinal.data.local

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DatosClinica(
    val nombre: String = "Veterinaria Huellitas",
    val direccion: String = "Comayagüela, Honduras",
    val telefono: String = "",
    val horario: String = "",
)

class ClinicaStore(context: Context) {

    private val prefs = context.getSharedPreferences("petcare_clinica", Context.MODE_PRIVATE)

    private val _datos = MutableStateFlow(leer())
    val datos: StateFlow<DatosClinica> = _datos.asStateFlow()

    private fun leer(): DatosClinica {
        val base = DatosClinica()
        return DatosClinica(
            nombre = prefs.getString(NOMBRE, null) ?: base.nombre,
            direccion = prefs.getString(DIRECCION, null) ?: base.direccion,
            telefono = prefs.getString(TELEFONO, null) ?: base.telefono,
            horario = prefs.getString(HORARIO, null) ?: base.horario,
        )
    }

    fun guardar(datos: DatosClinica) {
        prefs.edit {
            putString(NOMBRE, datos.nombre.trim())
            putString(DIRECCION, datos.direccion.trim())
            putString(TELEFONO, datos.telefono.trim())
            putString(HORARIO, datos.horario.trim())
        }
        _datos.value = leer()
    }

    private companion object {
        const val NOMBRE = "nombre"
        const val DIRECCION = "direccion"
        const val TELEFONO = "telefono"
        const val HORARIO = "horario"
    }
}
