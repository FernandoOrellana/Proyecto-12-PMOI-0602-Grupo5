package com.uth.proyectofinal.ui.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.data.remote.dto.Ref
import com.uth.proyectofinal.data.remote.dto.ServicioResumen
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.Page
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.PagedColumn
import com.uth.proyectofinal.ui.common.PagedState
import com.uth.proyectofinal.ui.common.PagedViewModel
import com.uth.proyectofinal.ui.common.aFechaIso
import com.uth.proyectofinal.ui.common.aMillisUtc
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.isoFecha
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class AgendaViewModel(private val citas: CitaRepository) : PagedViewModel<Cita>() {

    private val _fecha = MutableStateFlow(isoFecha())
    val fecha: StateFlow<String> = _fecha.asStateFlow()

    override suspend fun fetch(page: Int): ApiResult<Page<List<Cita>>> =
        citas.list(_fecha.value, null, page, 50)

    fun cambiarFecha(nueva: String) {
        _fecha.value = nueva
        reload()
    }
}

@Composable
fun AgendaScreen(onOpen: (Int) -> Unit) {
    val vm: AgendaViewModel = viewModel(factory = appFactory { AgendaViewModel(it.citas) })
    val state by vm.state.collectAsStateWithLifecycle()
    val fecha by vm.fecha.collectAsStateWithLifecycle()

    OnResume { vm.refresh() }

    AgendaContent(
        state = state,
        fecha = fecha,
        messages = vm.messages,
        onCambiarFecha = vm::cambiarFecha,
        onLoadMore = vm::loadMore,
        onRefresh = vm::pullRefresh,
        onOpen = onOpen,
    )
}

@Composable
fun AgendaContent(
    state: PagedState<Cita>,
    fecha: String,
    messages: Flow<String>? = null,
    onCambiarFecha: (String) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
    onOpen: (Int) -> Unit = {},
) {
    var mostrarCalendario by remember { mutableStateOf(false) }

    AppScaffold(title = "Agenda", messages = messages) { padding ->
        Column(Modifier.padding(padding)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Agenda del $fecha", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { mostrarCalendario = true }, modifier = Modifier.weight(1f)) {
                        Text("Elegir fecha")
                    }
                    OutlinedButton(onClick = { onCambiarFecha(isoFecha()) }) { Text("Hoy") }
                }
            }
            PagedColumn(
                state = state,
                emptyText = "No hay citas para esta fecha.",
                onLoadMore = onLoadMore,
                onRefresh = onRefresh,
            ) { cita ->
                Card(onClick = { onOpen(cita.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cita.hora, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            EstadoBadge(cita.estado)
                        }
                        Text("Mascota: ${cita.mascota.nombre} (dueño: ${cita.dueno?.nombre.orEmpty()})")
                        Text("Servicio: ${cita.servicio.nombre}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (mostrarCalendario) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = fecha.aMillisUtc())
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onCambiarFecha(it.aFechaIso()) }
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = picker, title = { Text("Elige la fecha") })
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AgendaScreenPreview() {
    PetCareTheme {
        AgendaContent(
            state = PagedState(
                items = listOf(
                    Cita(
                        id = 1,
                        fecha = "2026-03-30",
                        hora = "09:00",
                        estado = "PENDIENTE",
                        mascota = Ref(1, "Max"),
                        servicio = ServicioResumen(1, "Consulta General", 350.0),
                        veterinario = Ref(1, "Dr. Carlos"),
                        dueno = Ref(1, "Juan Pérez"),
                    ),
                    Cita(
                        id = 2,
                        fecha = "2026-03-30",
                        hora = "10:30",
                        estado = "CONFIRMADA",
                        mascota = Ref(2, "Luna"),
                        servicio = ServicioResumen(2, "Vacunación", 250.0),
                        veterinario = Ref(1, "Dr. Carlos"),
                        dueno = Ref(2, "María López"),
                    )
                )
            ),
            fecha = "2026-03-30",
        )
    }
}
