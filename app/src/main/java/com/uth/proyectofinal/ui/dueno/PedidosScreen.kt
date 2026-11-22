package com.uth.proyectofinal.ui.dueno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.ItemPedido
import com.uth.proyectofinal.data.remote.dto.Pedido
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.Page
import com.uth.proyectofinal.data.repository.TiendaRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.PagedColumn
import com.uth.proyectofinal.ui.common.PagedState
import com.uth.proyectofinal.ui.common.PagedViewModel
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.lps
import com.uth.proyectofinal.ui.common.textoFecha
import com.uth.proyectofinal.ui.theme.PetCareTheme

class PedidosViewModel(private val tienda: TiendaRepository) : PagedViewModel<Pedido>() {
    override suspend fun fetch(page: Int): ApiResult<Page<List<Pedido>>> = tienda.pedidos(page)
}

@Composable
fun PedidosScreen(onOpen: (Int) -> Unit) {
    val vm: PedidosViewModel = viewModel(factory = appFactory { PedidosViewModel(it.tienda) })
    val state by vm.state.collectAsStateWithLifecycle()

    OnResume { vm.refresh() }

    PedidosContent(
        state = state,
        onOpen = onOpen,
        onLoadMore = vm::loadMore,
    )
}

@Composable
fun PedidosContent(
    state: PagedState<Pedido>,
    onOpen: (Int) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    AppScaffold(title = "Mis pedidos") { padding ->
        PagedColumn(
            state = state,
            emptyText = "Aún no tienes pedidos.",
            onLoadMore = onLoadMore,
            modifier = Modifier.padding(padding),
        ) { pedido ->
            Card(onClick = { onOpen(pedido.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pedido #${pedido.id}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        EstadoBadge(pedido.estado)
                    }
                    Text(pedido.fecha.textoFecha(), style = MaterialTheme.typography.bodySmall)
                    Text("Total: ${pedido.total.lps()} · ${pedido.items.size} producto(s)")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PedidosScreenPreview() {
    PetCareTheme {
        PedidosContent(
            state = PagedState(
                items = listOf(
                    Pedido(
                        id = 101,
                        fecha = "2026-03-30T10:00:00Z",
                        estado = "PENDIENTE",
                        total = 450.0,
                        items = listOf(
                            ItemPedido(productoId = 1, nombre = "Alimento Canino 5kg", precioUnitario = 450.0, cantidad = 1, subtotal = 450.0),
                        ),
                    ),
                ),
            ),
        )
    }
}
