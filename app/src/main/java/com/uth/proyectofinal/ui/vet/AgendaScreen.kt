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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.uth.proyectofinal.data.remote.dto.CitaEstado
import com.uth.proyectofinal.ui.common.DiasSelector
import com.uth.proyectofinal.ui.common.fechaCorta
import com.uth.proyectofinal.ui.common.textoCuando
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.luminance

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

    val hoy = isoFecha()
    val ahora = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    val primeraFutura = if (fecha == hoy) state.items.firstOrNull { it.hora >= ahora }?.id else null

    AppScaffold(
        title = "Agenda",
        messages = messages,
        actions = {
            if (fecha != hoy) TextButton(onClick = { onCambiarFecha(hoy) }) { Text("Hoy") }
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            DiasSelector(
                fecha = fecha,
                onFecha = onCambiarFecha,
                desde = -3,
                dias = 17,
                onOtraFecha = { mostrarCalendario = true },
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                "${textoCuando(fecha)} · ${fechaCorta(fecha)} · ${state.items.size} citas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            PagedColumn(
                state = state,
                emptyText = "No hay citas para esta fecha.",
                onLoadMore = onLoadMore,
                onRefresh = onRefresh,
            ) { cita ->
                Column {
                    if (cita.id == primeraFutura) LineaAhora(ahora)
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            cita.hora,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(52.dp).padding(top = 14.dp),
                        )
                        Card(
                            onClick = { onOpen(cita.id) },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = fondoCita(cita.estado)),
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${cita.mascota.nombre} · ${cita.servicio.nombre}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    EstadoBadge(cita.estado)
                                }
                                Text(
                                    cita.dueno?.nombre.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
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

@Composable
private fun LineaAhora(hora: String) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            hora,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.width(52.dp),
        )
        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
        Box(Modifier.weight(1f).height(2.dp).background(MaterialTheme.colorScheme.error))
    }
}

@Composable
private fun fondoCita(estado: String): Color {
    val oscuro = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (estado) {
        CitaEstado.ATENDIDA -> if (oscuro) Color(0xFF1C3A2E) else Color(0xFFE3F5EC)
        CitaEstado.EN_CONSULTA -> if (oscuro) Color(0xFF1B2F45) else Color(0xFFE6F0FB)
        CitaEstado.CANCELADA -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
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
