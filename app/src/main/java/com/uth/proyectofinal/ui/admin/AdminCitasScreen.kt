package com.uth.proyectofinal.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.data.remote.dto.CitaEstado
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
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val estadosAdmin = listOf(
    CitaEstado.PENDIENTE to "Pendientes",
    CitaEstado.EN_CONSULTA to "En consulta",
    CitaEstado.ATENDIDA to "Atendidas",
    CitaEstado.CANCELADA to "Canceladas",
)

class AdminCitasViewModel(private val citas: CitaRepository) : PagedViewModel<Cita>() {
    private val _estado = MutableStateFlow(CitaEstado.PENDIENTE)
    val estado: StateFlow<String> = _estado.asStateFlow()

    override suspend fun fetch(page: Int): ApiResult<Page<List<Cita>>> {
        val seleccionado = _estado.value
        val result = citas.list(null, seleccionado, page)
        if (result !is ApiResult.Success || seleccionado != CitaEstado.ATENDIDA) return result
        val items = coroutineScope {
            result.data.data.map { cita ->
                async {
                    if (cita.estado != CitaEstado.ATENDIDA) cita
                    else when (val cobro = citas.cobroCita(cita.id)) {
                        is ApiResult.Success -> cita.copy(cobrada = cobro.data != null)
                        is ApiResult.Error -> cita
                    }
                }
            }.awaitAll()
        }
        return ApiResult.Success(Page(items, result.data.meta))
    }

    fun cambiarEstado(nuevo: String) {
        if (nuevo == _estado.value) return
        _estado.value = nuevo
        refresh()
    }
}

@Composable
fun AdminCitasScreen(onOpen: (Int) -> Unit) {
    val vm: AdminCitasViewModel = viewModel(factory = appFactory { AdminCitasViewModel(it.citas) })
    val state by vm.state.collectAsStateWithLifecycle()
    val estado by vm.estado.collectAsStateWithLifecycle()

    OnResume { vm.refresh() }

    AdminCitasContent(
        state = state,
        estado = estado,
        onCambiarEstado = vm::cambiarEstado,
        onOpen = onOpen,
        onLoadMore = vm::loadMore,
    )
}

@Composable
fun AdminCitasContent(
    state: PagedState<Cita>,
    estado: String,
    onCambiarEstado: (String) -> Unit = {},
    onOpen: (Int) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    AppScaffold(title = "Citas") { padding ->
        Column(Modifier.padding(padding)) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                estadosAdmin.forEach { (valor, texto) ->
                    FilterChip(
                        selected = estado == valor,
                        onClick = { onCambiarEstado(valor) },
                        label = { Text(texto) },
                    )
                }
            }
            val etiqueta = estadosAdmin.firstOrNull { it.first == estado }?.second?.lowercase() ?: "filtradas"
            PagedColumn(
                state = state,
                emptyText = "No hay citas $etiqueta.",
                onLoadMore = onLoadMore,
                modifier = Modifier.weight(1f),
            ) { cita ->
                Card(onClick = { onOpen(cita.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Cita #${cita.id} · ${cita.fecha} ${cita.hora}", style = MaterialTheme.typography.titleMedium)
                        Text("Cliente: ${cita.dueno?.nombre ?: "No disponible"}")
                        Text("Mascota: ${cita.mascota.nombre} · ${cita.servicio.nombre}")
                        Text("Veterinario: ${cita.veterinario.nombre.ifEmpty { "No asignado" }}")
                        EstadoBadge(
                            when {
                                cita.estado == CitaEstado.ATENDIDA && cita.cobrada -> "ATENDIDA · YA PAGADO"
                                cita.estado == CitaEstado.ATENDIDA -> "POR COBRAR"
                                else -> cita.estado
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminCitasScreenPreview() {
    PetCareTheme {
        AdminCitasContent(
            state = PagedState(
                items = listOf(
                    Cita(
                        id = 1,
                        fecha = "2026-03-30",
                        hora = "09:00",
                        estado = CitaEstado.PENDIENTE,
                        mascota = Ref(1, "Max"),
                        servicio = ServicioResumen(1, "Consulta General", 350.0),
                        veterinario = Ref(1, "Dr. Carlos Rodríguez"),
                        dueno = Ref(1, "Juan Pérez"),
                    ),
                    Cita(
                        id = 2,
                        fecha = "2026-03-30",
                        hora = "10:30",
                        estado = CitaEstado.EN_CONSULTA,
                        mascota = Ref(2, "Luna"),
                        servicio = ServicioResumen(2, "Vacunación", 250.0),
                        veterinario = Ref(2, "Dra. Ana López"),
                        dueno = Ref(2, "María Gómez"),
                    ),
                ),
            ),
            estado = CitaEstado.PENDIENTE,
        )
    }
}
