package com.uth.proyectofinal.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Pedido
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.Page
import com.uth.proyectofinal.data.repository.TiendaRepository
import com.uth.proyectofinal.ui.common.*

class AdminPedidosViewModel(private val tienda: TiendaRepository) : PagedViewModel<Pedido>() {
    override suspend fun fetch(page: Int): ApiResult<Page<List<Pedido>>> = tienda.pedidos(page)
}

@Composable
fun AdminPedidosScreen(onOpen: (Int) -> Unit) {
    val vm: AdminPedidosViewModel = viewModel(factory = appFactory { AdminPedidosViewModel(it.tienda) })
    val state by vm.state.collectAsStateWithLifecycle()
    OnResume { vm.refresh() }
    AppScaffold(title = "Pedidos") { padding ->
        PagedColumn(state = state, emptyText = "No hay pedidos.", onLoadMore = vm::loadMore, onRefresh = vm::pullRefresh, modifier = Modifier.padding(padding)) { pedido ->
            Card(onClick = { onOpen(pedido.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pedido #${pedido.id}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        EstadoBadge(pedido.estado.replace('_', ' '))
                    }
                    Text("Cliente: ${pedido.cliente?.nombre ?: "No disponible"}")
                    Text(pedido.fecha.textoFecha(), style = MaterialTheme.typography.bodySmall)
                    Text("Total: ${pedido.total.lps()}")
                }
            }
        }
    }
}
