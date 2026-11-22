package com.uth.proyectofinal.ui.dueno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.uth.proyectofinal.data.local.CartStore
import com.uth.proyectofinal.data.remote.dto.Producto
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.Page
import com.uth.proyectofinal.data.repository.TiendaRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.PagedState
import com.uth.proyectofinal.ui.common.PagedViewModel
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.lps
import com.uth.proyectofinal.ui.common.opcional
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TiendaViewModel(
    private val tienda: TiendaRepository,
    private val cart: CartStore,
) : PagedViewModel<Producto>() {

    private val _categoria = MutableStateFlow("")

    val cantidadCarrito: StateFlow<Int> = cart.items
        .map { items -> items.sumOf { it.cantidad } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    override suspend fun fetch(page: Int): ApiResult<Page<List<Producto>>> =
        tienda.productos(_categoria.value.opcional(), page)

    fun filtrar(categoria: String) {
        _categoria.value = categoria.trim()
        reload()
    }

    fun agregar(producto: Producto) {
        if (cart.add(producto)) {
            say("${producto.nombre} agregado al carrito.")
        } else {
            say("${producto.nombre} no tiene stock disponible.")
        }
    }
}

@Composable
fun TiendaScreen(onCarrito: () -> Unit) {
    val vm: TiendaViewModel = viewModel(factory = appFactory { TiendaViewModel(it.tienda, it.cart) })
    val state by vm.state.collectAsStateWithLifecycle()
    val cantidad by vm.cantidadCarrito.collectAsStateWithLifecycle()

    OnResume { vm.refresh() }

    TiendaContent(
        state = state,
        cantidadCarrito = cantidad,
        messages = vm.messages,
        onCarrito = onCarrito,
        onFiltrar = vm::filtrar,
        onAgregar = vm::agregar,
        onLoadMore = vm::loadMore,
    )
}

@Composable
fun TiendaContent(
    state: PagedState<Producto>,
    cantidadCarrito: Int = 0,
    messages: Flow<String>? = null,
    onCarrito: () -> Unit = {},
    onFiltrar: (String) -> Unit = {},
    onAgregar: (Producto) -> Unit = {},
    onLoadMore: () -> Unit = {},
) {
    var categoria by remember { mutableStateOf("") }

    AppScaffold(
        title = "Tienda",
        messages = messages,
        actions = {
            IconButton(onClick = onCarrito) {
                BadgedBox(badge = { if (cantidadCarrito > 0) Badge { Text("$cantidadCarrito") } }) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = categoria,
                    onValueChange = { categoria = it },
                    label = { Text("Categoría") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onFiltrar(categoria) }) { Text("Filtrar") }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.items) { p ->
                    ProductoCard(p) { onAgregar(p) }
                }
                footer(state.loading, state.error, state.hasMore, state.items.isEmpty(), onLoadMore)
            }
        }
    }
}

private fun LazyGridScope.footer(
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    vacio: Boolean,
    onLoadMore: () -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) CircularProgressIndicator(Modifier.size(24.dp))
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            if (!loading && vacio && error == null) Text("No hay productos disponibles.")
            if (!loading && hasMore) TextButton(onClick = onLoadMore) { Text("Cargar más") }
        }
    }
}

@Composable
private fun ProductoCard(p: Producto, onAgregar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            AsyncImage(
                model = p.imagenUrl,
                contentDescription = p.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(p.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(p.precio.lps(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Stock: ${p.stock}", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onAgregar, enabled = p.stock > 0, modifier = Modifier.fillMaxWidth()) {
                    Text(if (p.stock > 0) "Agregar" else "Agotado")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TiendaScreenPreview() {
    PetCareTheme {
        TiendaContent(
            state = PagedState(
                items = listOf(
                    Producto(id = 1, nombre = "Alimento Canino 5kg", precio = 450.0, stock = 10, categoria = "Alimentos"),
                    Producto(id = 2, nombre = "Champú para Perros 500ml", precio = 180.0, stock = 5, categoria = "Higiene"),
                ),
            ),
            cantidadCarrito = 2,
        )
    }
}
