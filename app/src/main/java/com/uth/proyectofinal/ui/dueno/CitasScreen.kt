package com.uth.proyectofinal.ui.dueno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.data.remote.dto.CitaEstado
import com.uth.proyectofinal.data.remote.dto.Ref
import com.uth.proyectofinal.data.remote.dto.ServicioResumen
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.Page
import com.uth.proyectofinal.ui.common.AddFab
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.ConfirmDialog
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.PagedColumn
import com.uth.proyectofinal.ui.common.PagedState
import com.uth.proyectofinal.ui.common.PagedViewModel
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.lps
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CitasViewModel(private val repo: CitaRepository) : PagedViewModel<Cita>() {

    override suspend fun fetch(page: Int): ApiResult<Page<List<Cita>>> = repo.list(null, null, page)

    fun cancelar(id: Int) {
        viewModelScope.launch {
            when (val result = repo.cancelar(id)) {
                is ApiResult.Success -> {
                    say("Cita cancelada.")
                    refresh()
                }
                is ApiResult.Error -> say(result.message)
            }
        }
    }
}

@Composable
fun CitasScreen(onNueva: () -> Unit) {
    val vm: CitasViewModel = viewModel(factory = appFactory { CitasViewModel(it.citas) })
    val state by vm.state.collectAsStateWithLifecycle()

    OnResume { vm.refresh() }

    CitasContent(
        state = state,
        messages = vm.messages,
        onNueva = onNueva,
        onLoadMore = vm::loadMore,
        onCancelar = vm::cancelar,
    )
}

@Composable
fun CitasContent(
    state: PagedState<Cita>,
    messages: Flow<String>? = null,
    onNueva: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onCancelar: (Int) -> Unit = {},
) {
    var aCancelar by remember { mutableStateOf<Cita?>(null) }

    AppScaffold(
        title = "Mis citas",
        messages = messages,
        floatingActionButton = { AddFab("Agendar cita", onNueva) },
    ) { padding ->
        PagedColumn(
            state = state,
            emptyText = "No tienes citas. Usa + para agendar una.",
            onLoadMore = onLoadMore,
            modifier = Modifier.padding(padding),
        ) { cita ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${cita.fecha} ${cita.hora}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        EstadoBadge(cita.estado)
                    }
                    Text("${cita.mascota.nombre} · ${cita.servicio.nombre} (${cita.servicio.precio.lps()})")
                    Text("Veterinario: ${cita.veterinario.nombre}", style = MaterialTheme.typography.bodySmall)
                    if (cita.estado == CitaEstado.PENDIENTE || cita.estado == CitaEstado.CONFIRMADA) {
                        TextButton(onClick = { aCancelar = cita }) { Text("Cancelar cita") }
                    }
                }
            }
        }
    }

    aCancelar?.let { cita ->
        ConfirmDialog(
            title = "Cancelar cita",
            text = "¿Cancelar la cita de ${cita.mascota.nombre} el ${cita.fecha} a las ${cita.hora}?",
            confirmText = "Sí, cancelar",
            onConfirm = {
                aCancelar = null
                onCancelar(cita.id)
            },
            onDismiss = { aCancelar = null },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CitasScreenPreview() {
    PetCareTheme {
        CitasContent(
            state = PagedState(
                items = listOf(
                    Cita(
                        id = 1,
                        fecha = "2026-03-30",
                        hora = "10:00",
                        estado = CitaEstado.PENDIENTE,
                        mascota = Ref(1, "Max"),
                        servicio = ServicioResumen(1, "Consulta General", 350.0),
                        veterinario = Ref(1, "Dr. Carlos"),
                    ),
                    Cita(
                        id = 2,
                        fecha = "2026-03-25",
                        hora = "14:00",
                        estado = CitaEstado.ATENDIDA,
                        mascota = Ref(2, "Luna"),
                        servicio = ServicioResumen(2, "Desparasitaciones", 200.0),
                        veterinario = Ref(2, "Dra. Ana"),
                    ),
                ),
            ),
        )
    }
}
