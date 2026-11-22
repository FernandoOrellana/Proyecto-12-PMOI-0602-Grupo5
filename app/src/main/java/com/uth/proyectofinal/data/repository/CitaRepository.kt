package com.uth.proyectofinal.data.repository

import com.uth.proyectofinal.data.remote.ApiService
import com.uth.proyectofinal.data.remote.dto.CitaRequest
import com.uth.proyectofinal.data.remote.dto.ConsultaRequest
import com.uth.proyectofinal.data.remote.dto.ComprobanteInterno
import com.uth.proyectofinal.data.remote.dto.RegistrarCobroRequest
import com.uth.proyectofinal.data.remote.ApiResponse
import com.google.gson.Gson
import com.google.gson.JsonParseException
import java.io.IOException

class CitaRepository(private val api: ApiService) {

    suspend fun list(fecha: String?, estado: String?, page: Int, limit: Int = 20) =
        apiPage { api.citas(fecha, estado, page, limit) }

    suspend fun get(id: Int) = apiCall { api.cita(id) }

    suspend fun crear(body: CitaRequest) = apiCall { api.crearCita(body) }

    suspend fun cancelar(id: Int) = apiCall { api.cancelarCita(id) }

    suspend fun iniciarConsulta(id: Int) = apiCall { api.iniciarConsulta(id) }

    suspend fun registrarConsulta(id: Int, body: ConsultaRequest) =
        apiCall { api.registrarConsulta(id, body) }

    suspend fun agregarObservacion(id: Int, observaciones: String) =
        apiCall { api.agregarObservacion(id, com.uth.proyectofinal.data.remote.dto.ObservacionesRequest(observaciones)) }

    suspend fun cobrarCita(id: Int, request: RegistrarCobroRequest) = apiCall { api.cobrarCita(id, request) }

    suspend fun cobroCita(id: Int): ApiResult<ComprobanteInterno?> = try {
        val response = api.cobroCita(id)
        val body = response.body()
        if (response.isSuccessful && body?.success == true && body.data != null) {
            ApiResult.Success(body.data)
        } else if (response.code() == 404) {
            val error = response.errorBody()?.string()?.let { Gson().fromJson(it, ApiResponse::class.java)?.error }
            if (error?.code == "NOT_FOUND" && error.message == "Cobro no encontrado") ApiResult.Success(null)
            else ApiResult.Error("No se pudo consultar el cobro. Verifica que la API de cobros esté disponible.")
        } else {
            ApiResult.Error("No se pudo consultar el cobro. Intenta de nuevo.")
        }
    } catch (e: IOException) {
        ApiResult.Error("Sin conexión con el servidor. Revisa tu internet.")
    } catch (e: JsonParseException) {
        ApiResult.Error("La respuesta del servidor no es válida.")
    }
}
