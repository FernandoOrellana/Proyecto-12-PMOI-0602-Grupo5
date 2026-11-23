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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.uth.proyectofinal.data.repository.dataOrNull
import com.uth.proyectofinal.ui.common.EmptyBox
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.FiltroChips
import com.uth.proyectofinal.ui.common.LoadingBox
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TiendaViewModel(
    private val tienda: TiendaRepository,
    private val cart: CartStore,
) : PagedViewModel<Producto>() {

    private val _categoria = MutableStateFlow("")

    val cantidadCarrito: StateFlow<Int> = cart.items
        .map { items -> items.sumOf { it.cantidad } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val carrito: StateFlow<Map<Int, Int>> = cart.items
        .map { items -> items.associate { it.productoId to it.cantidad } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _categorias = MutableStateFlow<List<String>>(emptyList())
    val categorias: StateFlow<List<String>> = _categorias.asStateFlow()

    val categoria: StateFlow<String> = _categoria.asStateFlow()

    override suspend fun fetch(page: Int): ApiResult<Page<List<Producto>>> {
        val resultado = tienda.productos(_categoria.value.opcional(), page)
        resultado.dataOrNull()?.data?.let { lista ->
            _categorias.update { actuales ->
                (actuales + lista.map { it.categoria }).filter { it.isNotBlank() }.distinct().sorted()
            }
        }
        return resultado
    }

    fun cambiarCantidad(producto: Producto, cantidad: Int) {
        if (cantidad <= 0 || carrito.value.containsKey(producto.id)) {
            cart.setCantidad(producto.id, cantidad)
        } else {
            agregar(producto)
        }
    }

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
    val carrito by vm.carrito.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val categoria by vm.categoria.collectAsStateWithLifecycle()

    OnResume { vm.refresh() }

    TiendaContent(
        state = state,
        cantidadCarrito = cantidad,
        messages = vm.messages,
        onCarrito = onCarrito,
        onFiltrar = vm::filtrar,
        onAgregar = vm::agregar,
        onLoadMore = vm::loadMore,
        carrito = carrito,
        categorias = categorias,
        categoria = categoria,
        onCantidad = vm::cambiarCantidad,
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
    carrito: Map<Int, Int> = emptyMap(),
    categorias: List<String> = emptyList(),
    categoria: String = "",
    onCantidad: (Producto, Int) -> Unit = { _, _ -> },
) {
    var busqueda by remember { mutableStateOf("") }
    val visibles = if (busqueda.isBlank()) state.items else state.items.filter { it.nombre.contains(busqueda.trim(), ignoreCase = true) }

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
            OutlinedTextField(
                value = busqueda,
                onValueChange = { busqueda = it },
                placeholder = { Text("Buscar productos") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            if (categorias.isNotEmpty()) {
                FiltroChips(
                    opciones = listOf(TODO_CATEGORIAS) + categorias,
                    seleccion = categoria.ifBlank { TODO_CATEGORIAS },
                    onSelect = { onFiltrar(if (it == TODO_CATEGORIAS) "" else it) },
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            when {
                state.items.isEmpty() && state.loading -> LoadingBox()
                state.items.isEmpty() && state.error != null -> ErrorBox(state.error, onLoadMore)
                visibles.isEmpty() && !state.loading -> EmptyBox(
                    if (busqueda.isBlank()) "No hay productos disponibles." else "No encontramos \"${busqueda.trim()}\".",
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(visibles, key = { it.id }) { p ->
                        ProductoCard(
                            p = p,
                            cantidad = carrito[p.id] ?: 0,
                            onAgregar = { onAgregar(p) },
                            onCantidad = { onCantidad(p, it) },
                        )
                    }
                    footer(state.loading && !state.recargando, state.error, state.hasMore && busqueda.isBlank(), onLoadMore)
                }
            }
        }
    }
}

private const val TODO_CATEGORIAS = "Todo"

private fun LazyGridScope.footer(
    loading: Boolean,
    error: String?,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            if (!loading && hasMore && error == null) {
                LaunchedEffect(Unit) { onLoadMore() }
            }
        }
    }
}

@Composable
private fun ProductoCard(p: Producto, cantidad: Int, onAgregar: () -> Unit, onCantidad: (Int) -> Unit) {
    val agotado = p.stock <= 0
    Card(modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = if (agotado) 0.55f else 1f }) {
        Column {
            Box(
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (p.imagenUrl.isNullOrBlank()) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp),
                    )
                } else {
                    AsyncImage(
                        model = p.imagenUrl,
                        contentDescription = p.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    p.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        p.precio.lps(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    when {
                        agotado -> Text("Agotado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        cantidad == 0 -> FilledIconButton(
                            onClick = onAgregar,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar ${p.nombre}", modifier = Modifier.size(18.dp))
                        }
                        else -> Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedIconButton(onClick = { onCantidad(cantidad - 1) }, modifier = Modifier.size(28.dp)) {
                                Text("−", style = MaterialTheme.typography.titleSmall)
                            }
                            Text("$cantidad", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 8.dp))
                            OutlinedIconButton(
                                onClick = { onCantidad(cantidad + 1) },
                                enabled = cantidad < p.stock,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Sumar", modifier = Modifier.size(16.dp))
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
