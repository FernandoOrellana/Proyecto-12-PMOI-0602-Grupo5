package com.uth.proyectofinal.ui.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.data.remote.dto.CitaEstado
import com.uth.proyectofinal.data.remote.dto.Ref
import com.uth.proyectofinal.data.remote.dto.ServicioResumen
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.lps
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.uth.proyectofinal.ui.common.ContactoCliente
import com.uth.proyectofinal.ui.common.datosClinica

data class CitaDetailUi(
    val cita: Cita? = null,
    val cargando: Boolean = true,
    val error: String? = null,
    val iniciando: Boolean = false,
    val errorAccion: String? = null,
)

class CitaDetailViewModel(private val id: Int, private val citas: CitaRepository) : ViewModel() {

    private val _ui = MutableStateFlow(CitaDetailUi())
    val ui: StateFlow<CitaDetailUi> = _ui.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            when (val result = citas.get(id)) {
                is ApiResult.Success -> _ui.update { it.copy(cita = result.data, cargando = false) }
                is ApiResult.Error -> _ui.update { it.copy(cargando = false, error = result.message) }
            }
        }
    }

    fun iniciarConsulta() {
        val cita = _ui.value.cita ?: return
        if (_ui.value.iniciando) return
        viewModelScope.launch {
            _ui.update { it.copy(iniciando = true, errorAccion = null) }
            when (val result = citas.iniciarConsulta(cita.id)) {
                is ApiResult.Success -> _ui.update { it.copy(cita = result.data, iniciando = false) }
                is ApiResult.Error -> _ui.update { it.copy(iniciando = false, errorAccion = result.message) }
            }
        }
    }
}

@Composable
fun CitaDetailScreen(
    id: Int,
    onBack: () -> Unit,
    onConsulta: (Int) -> Unit,
    onVacuna: (Int, Int) -> Unit,
    onHistorialMascota: (Int) -> Unit,
) {
    val vm: CitaDetailViewModel = viewModel(factory = appFactory { CitaDetailViewModel(id, it.citas) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    OnResume { vm.cargar() }

    CitaDetailContent(
        ui = ui,
        onBack = onBack,
        onRetry = vm::cargar,
        onIniciar = vm::iniciarConsulta,
        onConsulta = onConsulta,
        onVacuna = onVacuna,
        onHistorialMascota = onHistorialMascota,
    )
}

@Composable
fun CitaDetailContent(
    ui: CitaDetailUi,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onIniciar: () -> Unit = {},
    onConsulta: (Int) -> Unit = {},
    onVacuna: (Int, Int) -> Unit = { _, _ -> },
    onHistorialMascota: (Int) -> Unit = {},
) {
    AppScaffold(title = "Detalle de cita", onBack = onBack) { padding ->
        val cita = ui.cita
        when {
            ui.cargando && cita == null -> LoadingBox(Modifier.padding(padding))
            cita == null -> ErrorBox(ui.error ?: "No se pudo cargar la cita.", onRetry, Modifier.padding(padding))
            else -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("${cita.fecha} · ${cita.hora}", style = MaterialTheme.typography.headlineSmall)
                EstadoBadge(cita.estado)
                Text("Mascota: ${cita.mascota.nombre}", style = MaterialTheme.typography.bodyLarge)
                Text("Dueño: ${cita.dueno?.nombre.orEmpty()}", style = MaterialTheme.typography.bodyLarge)
                ContactoCliente(
                    nombre = cita.dueno?.nombre.orEmpty(),
                    telefono = cita.dueno?.telefono,
                    mensaje = "Hola ${cita.dueno?.nombre.orEmpty()}, le escribimos de ${datosClinica().nombre} sobre la cita de ${cita.mascota.nombre} del ${cita.fecha} a las ${cita.hora}.",
                )
                Text("Servicio: ${cita.servicio.nombre} (${cita.servicio.precio.lps()})", style = MaterialTheme.typography.bodyLarge)
                Text("Veterinario: ${cita.veterinario.nombre}", style = MaterialTheme.typography.bodyLarge)

                Button(
                    onClick = { onHistorialMascota(cita.mascota.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Ver historial de citas de la mascota")
                }
                ui.errorAccion?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                when (cita.estado) {
                    CitaEstado.EN_CONSULTA -> Button(
                        onClick = { onConsulta(cita.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Finalizar consulta")
                    }
                    CitaEstado.PENDIENTE, CitaEstado.CONFIRMADA -> Button(
                        onClick = onIniciar,
                        enabled = !ui.iniciando,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (ui.iniciando) "Iniciando..." else "Iniciar consulta")
                    }
                }
                OutlinedButton(
                    onClick = { onVacuna(cita.id, cita.mascota.id) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Registrar vacuna")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CitaDetailScreenPreview() {
    PetCareTheme {
        CitaDetailContent(
            ui = CitaDetailUi(
                cita = Cita(
                    id = 1,
                    fecha = "2026-03-30",
                    hora = "10:00",
                    estado = "CONFIRMADA",
                    mascota = Ref(1, "Max"),
                    servicio = ServicioResumen(1, "Consulta General", 350.0),
                    veterinario = Ref(1, "Dr. Carlos"),
                    dueno = Ref(1, "Juan Pérez"),
                ),
                cargando = false,
            ),
        )
    }
}
