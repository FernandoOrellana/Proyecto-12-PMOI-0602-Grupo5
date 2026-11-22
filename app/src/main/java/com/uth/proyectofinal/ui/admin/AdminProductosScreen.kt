package com.uth.proyectofinal.ui.admin

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Producto
import com.uth.proyectofinal.data.repository.AdminRepository
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.Page
import com.uth.proyectofinal.data.repository.TiendaRepository
import com.uth.proyectofinal.ui.common.AddFab
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.ConfirmDialog
import com.uth.proyectofinal.ui.common.DeleteIcon
import com.uth.proyectofinal.ui.common.EditIcon
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.PagedColumn
import com.uth.proyectofinal.ui.common.PagedViewModel
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.lps
import kotlinx.coroutines.launch

class AdminProductosViewModel(
    private val tienda: TiendaRepository,
    private val admin: AdminRepository,
) : PagedViewModel<Producto>() {

    override suspend fun fetch(page: Int): ApiResult<Page<List<Producto>>> = tienda.productos(null, page)

    fun eliminar(id: Int) {
        viewModelScope.launch {
            when (val result = admin.eliminarProducto(id)) {
                is ApiResult.Success -> {
                    say("Producto eliminado.")
                    refresh()
                }
                is ApiResult.Error -> say(result.message)
            }
        }
    }
}

@Composable
fun AdminProductosScreen(onAdd: () -> Unit, onEdit: (Int) -> Unit) {
    val vm: AdminProductosViewModel = viewModel(
        factory = appFactory { AdminProductosViewModel(it.tienda, it.admin) },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var aEliminar by remember { mutableStateOf<Producto?>(null) }

    OnResume { vm.refresh() }

    AppScaffold(
        title = "Productos",
        messages = vm.messages,
        floatingActionButton = { AddFab("Nuevo producto", onAdd) },
    ) { padding ->
        PagedColumn(
            state = state,
            emptyText = "No hay productos. Usa + para crear uno.",
            onLoadMore = vm::loadMore, onRefresh = vm::pullRefresh,
            modifier = Modifier.padding(padding),
        ) { p ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(p.nombre, style = MaterialTheme.typography.titleMedium)
                        Text("${p.categoria} · ${p.precio.lps()} · Stock ${p.stock}", style = MaterialTheme.typography.bodySmall)
                    }
                    EditIcon("Editar producto") { onEdit(p.id) }
                    DeleteIcon("Eliminar producto") { aEliminar = p }
                }
            }
        }
    }

    aEliminar?.let { p ->
        ConfirmDialog(
            title = "Eliminar producto",
            text = "¿Eliminar ${p.nombre} del catálogo?",
            confirmText = "Eliminar",
            onConfirm = {
                aEliminar = null
                vm.eliminar(p.id)
            },
            onDismiss = { aEliminar = null },
        )
    }
}
