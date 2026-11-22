package com.uth.proyectofinal.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Notificacion
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.NotificacionRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.MessageViewModel
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.PagedColumn
import com.uth.proyectofinal.ui.common.PagedState
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.textoFecha
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificacionesViewModel(private val repo: NotificacionRepository) : MessageViewModel() {

    private val _state = MutableStateFlow(PagedState<Notificacion>(hasMore = false))
    val state: StateFlow<PagedState<Notificacion>> = _state.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = repo.list()) {
                is ApiResult.Success -> _state.update { it.copy(items = result.data, loading = false, hasMore = false) }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun probar() {
        viewModelScope.launch {
            when (val result = repo.probar()) {
                is ApiResult.Success -> {
                    say(result.data.message ?: "Notificación de prueba enviada.")
                    cargar()
                }
                is ApiResult.Error -> say(result.message)
            }
        }
    }

    fun marcarLeida(id: Int) {
        viewModelScope.launch {
            if (repo.marcarLeida(id) is ApiResult.Success) {
                _state.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(leida = true) else it }) }
            }
        }
    }
}

@Composable
fun NotificacionesScreen(onBack: () -> Unit) {
    val vm: NotificacionesViewModel = viewModel(factory = appFactory { NotificacionesViewModel(it.notificaciones) })
    val state by vm.state.collectAsStateWithLifecycle()

    OnResume { vm.cargar() }

    NotificacionesContent(
        state = state,
        messages = vm.messages,
        onBack = onBack,
        onProbar = vm::probar,
        onLoadMore = vm::cargar,
        onMarcarLeida = vm::marcarLeida,
    )
}

@Composable
fun NotificacionesContent(
    state: PagedState<Notificacion>,
    messages: Flow<String>? = null,
    onBack: () -> Unit = {},
    onProbar: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onMarcarLeida: (Int) -> Unit = {},
) {
    AppScaffold(
        title = "Notificaciones",
        onBack = onBack,
        messages = messages,
        actions = {
            IconButton(onClick = onProbar) {
                Icon(Icons.Filled.Notifications, contentDescription = "Enviar notificación de prueba")
            }
        },
    ) { padding ->
        PagedColumn(
            state = state,
            emptyText = "No tienes notificaciones.",
            onLoadMore = onLoadMore,
            modifier = Modifier.padding(padding),
        ) { n ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        n.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (n.leida) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    )
                    Text(n.mensaje, style = MaterialTheme.typography.bodyMedium)
                    Text(n.fecha.textoFecha(), style = MaterialTheme.typography.labelSmall)
                    if (!n.leida) {
                        TextButton(onClick = { onMarcarLeida(n.id) }) { Text("Marcar como leída") }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificacionesScreenPreview() {
    PetCareTheme {
        NotificacionesContent(
            state = PagedState(
                items = listOf(
                    Notificacion(
                        id = 1,
                        titulo = "Recordatorio de cita",
                        mensaje = "Tienes una cita mañana a las 10:00 AM.",
                        fecha = "2026-03-30T09:00:00Z",
                        leida = false,
                    ),
                    Notificacion(
                        id = 2,
                        titulo = "Resultado de vacuna",
                        mensaje = "Se ha registrado la vacuna de Rabia.",
                        fecha = "2026-03-29T15:30:00Z",
                        leida = true,
                    ),
                ),
            ),
        )
    }
}
