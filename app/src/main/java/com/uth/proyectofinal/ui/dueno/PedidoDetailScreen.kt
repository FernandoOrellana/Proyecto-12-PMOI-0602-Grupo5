package com.uth.proyectofinal.ui.dueno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.ItemPedido
import com.uth.proyectofinal.data.remote.dto.Pedido
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.TiendaRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.EmptyBox
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.lps
import com.uth.proyectofinal.ui.common.textoFecha
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PedidoDetailUi(
    val pedido: Pedido? = null,
    val cargando: Boolean = true,
    val error: String? = null,
)

class PedidoDetailViewModel(private val id: Int, private val tienda: TiendaRepository) : ViewModel() {

    private val _ui = MutableStateFlow(PedidoDetailUi())
    val ui: StateFlow<PedidoDetailUi> = _ui.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            when (val result = tienda.pedido(id)) {
                is ApiResult.Success -> _ui.update { it.copy(pedido = result.data, cargando = false) }
                is ApiResult.Error -> _ui.update { it.copy(cargando = false, error = result.message) }
            }
        }
    }
}

@Composable
fun PedidoDetailScreen(id: Int, onBack: () -> Unit) {
    val vm: PedidoDetailViewModel = viewModel(factory = appFactory { PedidoDetailViewModel(id, it.tienda) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    OnResume { vm.cargar() }

    PedidoDetailContent(
        id = id,
        ui = ui,
        onBack = onBack,
        onRetry = vm::cargar,
    )
}

@Composable
fun PedidoDetailContent(
    id: Int,
    ui: PedidoDetailUi,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    AppScaffold(title = "Pedido #$id", onBack = onBack) { padding ->
        val pedido = ui.pedido
        when {
            ui.cargando -> LoadingBox(Modifier.padding(padding))
            pedido == null -> ErrorBox(ui.error ?: "No se pudo cargar el pedido.", onRetry, Modifier.padding(padding))
            else -> Column(Modifier.padding(padding).fillMaxSize()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Estado", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        EstadoBadge(pedido.estado)
                    }
                    Text("Fecha: ${pedido.fecha.textoFecha()}")
                    Text("Total: ${pedido.total.lps()}", style = MaterialTheme.typography.titleMedium)
                    Text("Pague en mostrador al recoger su pedido.", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                if (pedido.items.isEmpty()) {
                    EmptyBox("El pedido no tiene productos.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pedido.items) { item ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(item.nombre, style = MaterialTheme.typography.titleSmall)
                                    Text("${item.cantidad} x ${item.precioUnitario.lps()} = ${item.subtotal.lps()}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PedidoDetailScreenPreview() {
    PetCareTheme {
        PedidoDetailContent(
            id = 101,
            ui = PedidoDetailUi(
                pedido = Pedido(
                    id = 101,
                    fecha = "2026-03-30T10:00:00Z",
                    estado = "CONFIRMADO",
                    total = 450.0,
                    items = listOf(
                        ItemPedido(productoId = 1, nombre = "Alimento Canino 5kg", precioUnitario = 450.0, cantidad = 1, subtotal = 450.0),
                    ),
                ),
                cargando = false,
            ),
        )
    }
}
