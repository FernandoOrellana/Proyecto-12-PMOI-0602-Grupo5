package com.uth.proyectofinal.ui.dueno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.CitaRequest
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CatalogRepository
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.data.repository.dataOrNull
import com.uth.proyectofinal.ui.common.ActionButton
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.ErrorText
import com.uth.proyectofinal.ui.common.Option
import com.uth.proyectofinal.ui.common.SelectField
import com.uth.proyectofinal.ui.common.aFechaIso
import com.uth.proyectofinal.ui.common.aMillisUtc
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.esFecha
import com.uth.proyectofinal.ui.common.isoFecha
import com.uth.proyectofinal.ui.common.lps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgendarUi(
    val mascotas: List<Option> = emptyList(),
    val servicios: List<Option> = emptyList(),
    val veterinarios: List<Option> = emptyList(),
    val mascotaId: Int? = null,
    val servicioId: Int? = null,
    val veterinarioId: Int? = null,
    val fecha: String = isoFecha(),
    val horas: List<String> = emptyList(),
    val hora: String? = null,
    val cargando: Boolean = true,
    val cargandoHoras: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val listo: Boolean = false,
)

class AgendarCitaViewModel(
    private val citas: CitaRepository,
    private val mascotas: MascotaRepository,
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AgendarUi())
    val ui: StateFlow<AgendarUi> = _ui.asStateFlow()

    init {
        cargarCatalogos()
    }

    private fun cargarCatalogos() {
        viewModelScope.launch {
            val mascotasRes = mascotas.list(1, 100)
            val serviciosRes = catalog.servicios()
            val vetsRes = catalog.veterinarios()
            val error = listOf(mascotasRes, serviciosRes, vetsRes)
                .filterIsInstance<ApiResult.Error>()
                .firstOrNull()?.message
            _ui.update {
                it.copy(
                    cargando = false,
                    error = error,
                    mascotas = mascotasRes.dataOrNull()?.data.orEmpty().map { m -> Option(m.id, m.nombre) },
                    servicios = serviciosRes.dataOrNull().orEmpty().map { s ->
                        Option(s.id, "${s.nombre} · ${s.precio.lps()}")
                    },
                    veterinarios = vetsRes.dataOrNull().orEmpty().map { v -> Option(v.id, v.nombre) },
                )
            }
        }
    }

    fun onMascota(id: Int) = _ui.update { it.copy(mascotaId = id, error = null) }

    fun onServicio(id: Int) = _ui.update { it.copy(servicioId = id, error = null) }

    fun onVeterinario(id: Int) {
        _ui.update { it.copy(veterinarioId = id, error = null) }
        refrescarHoras()
    }

    fun onFecha(value: String) {
        _ui.update { it.copy(fecha = value, error = null) }
        refrescarHoras()
    }

    fun onHora(value: String) = _ui.update { it.copy(hora = value, error = null) }

    private fun refrescarHoras() {
        val s = _ui.value
        val veterinario = s.veterinarioId
        if (veterinario == null || !s.fecha.esFecha()) {
            _ui.update { it.copy(horas = emptyList(), hora = null) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(cargandoHoras = true, hora = null, horas = emptyList()) }
            val result = catalog.disponibilidad(veterinario, s.fecha)
            _ui.update {
                it.copy(
                    cargandoHoras = false,
                    horas = result.dataOrNull().orEmpty(),
                    error = (result as? ApiResult.Error)?.message,
                )
            }
        }
    }

    fun confirmar() {
        val s = _ui.value
        val body = if (
            s.mascotaId != null && s.servicioId != null && s.veterinarioId != null &&
            s.hora != null && s.fecha.esFecha()
        ) {
            CitaRequest(s.mascotaId, s.servicioId, s.veterinarioId, s.fecha, s.hora)
        } else {
            null
        }
        if (body == null) {
            _ui.update { it.copy(error = "Completa mascota, servicio, veterinario, fecha y hora.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(guardando = true, error = null) }
            when (val result = citas.crear(body)) {
                is ApiResult.Success -> _ui.update { it.copy(guardando = false, listo = true) }
                is ApiResult.Error -> _ui.update { it.copy(guardando = false, error = result.message) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgendarCitaScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    val vm: AgendarCitaViewModel = viewModel(
        factory = appFactory { AgendarCitaViewModel(it.citas, it.mascotas, it.catalog) },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    var mostrarCalendario by remember { mutableStateOf(false) }

    LaunchedEffect(ui.listo) {
        if (ui.listo) onCreated()
    }

    AppScaffold(title = "Agendar cita", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (ui.cargando) {
                CircularProgressIndicator()
            }
            SelectField("Mascota", ui.mascotas, ui.mascotaId, vm::onMascota)
            SelectField("Servicio", ui.servicios, ui.servicioId, vm::onServicio)
            SelectField("Veterinario", ui.veterinarios, ui.veterinarioId, vm::onVeterinario)
            Text("Fecha: ${ui.fecha}", style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = { mostrarCalendario = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Elegir fecha")
            }
            Text("Horas disponibles", style = MaterialTheme.typography.titleSmall)
            when {
                ui.cargandoHoras -> CircularProgressIndicator(Modifier.size(24.dp))
                ui.veterinarioId == null || !ui.fecha.esFecha() ->
                    Text("Elige veterinario y fecha para ver horarios.", style = MaterialTheme.typography.bodyMedium)
                ui.horas.isEmpty() -> Text("No hay horarios disponibles para esa fecha.")
                else -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.horas.forEach { h ->
                        FilterChip(
                            selected = ui.hora == h,
                            onClick = { vm.onHora(h) },
                            label = { Text(h) },
                        )
                    }
                }
            }
            ErrorText(ui.error)
            ActionButton("Confirmar cita", ui.guardando, vm::confirmar)
        }
    }

    if (mostrarCalendario) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = ui.fecha.aMillisUtc(),
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { vm.onFecha(it.aFechaIso()) }
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = picker, title = { Text("Elige la fecha de la cita") })
        }
    }
}
