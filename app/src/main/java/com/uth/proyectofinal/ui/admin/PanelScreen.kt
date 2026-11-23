@file:OptIn(ExperimentalMaterial3Api::class)

package com.uth.proyectofinal.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.CitaEstado
import com.uth.proyectofinal.data.remote.dto.Producto
import com.uth.proyectofinal.data.repository.AdminRepository
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.TiendaRepository
import com.uth.proyectofinal.data.repository.dataOrNull
import com.uth.proyectofinal.ui.common.AccesoRapido
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.BarraSemanal
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.FilaNavegable
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.MetricaCard
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.Seccion
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.fechaMasDias
import com.uth.proyectofinal.ui.common.isoFecha
import com.uth.proyectofinal.ui.common.lps
import com.uth.proyectofinal.ui.common.nombreDia
import com.uth.proyectofinal.ui.common.primerDiaDelMes
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOCK_BAJO = 3

data class PanelUi(
    val citasHoy: Int = 0,
    val atendidasHoy: Int = 0,
    val porCobrar: Int = 0,
    val montoPorCobrar: Double = 0.0,
    val pedidosPendientes: Int = 0,
    val ventasMes: Double = 0.0,
    val pedidosMes: Int = 0,
    val semana: List<Pair<String, Int>> = emptyList(),
    val stockBajo: List<Producto> = emptyList(),
    val cargando: Boolean = true,
    val refrescando: Boolean = false,
    val cargado: Boolean = false,
    val error: String? = null,
)

class PanelViewModel(
    private val admin: AdminRepository,
    private val citas: CitaRepository,
    private val tienda: TiendaRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(PanelUi())
    val ui: StateFlow<PanelUi> = _ui.asStateFlow()

    fun cargar(mostrarIndicador: Boolean = false) {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, refrescando = mostrarIndicador, error = null) }
            val hoy = isoFecha()
            coroutineScope {
                val citasHoy = async { admin.reporteCitas(hoy, hoy) }
                val atendidas = async { citas.list(null, CitaEstado.ATENDIDA, 1, 100) }
                val pedidos = async { tienda.pedidos(1, 50) }
                val ventas = async { admin.reporteVentas(primerDiaDelMes(), hoy) }
                val productos = async { tienda.productos(null, 1, 100) }
                val semana = (-6..0).map { d ->
                    async {
                        val dia = fechaMasDias(d)
                        nombreDia(dia) to (admin.reporteCitas(dia, dia).dataOrNull()?.total ?: 0)
                    }
                }

                val reporteHoy = citasHoy.await()
                val sinCobrar = atendidas.await().dataOrNull()?.data.orEmpty().filter { !it.cobrada }
                val listaPedidos = pedidos.await().dataOrNull()?.data.orEmpty()
                val reporteVentas = ventas.await().dataOrNull()
                val listaProductos = productos.await().dataOrNull()?.data.orEmpty()
                val valoresSemana = semana.awaitAll()

                _ui.update {
                    it.copy(
                        citasHoy = reporteHoy.dataOrNull()?.total ?: 0,
                        atendidasHoy = reporteHoy.dataOrNull()?.porEstado?.get(CitaEstado.ATENDIDA) ?: 0,
                        porCobrar = sinCobrar.size,
                        montoPorCobrar = sinCobrar.sumOf { c -> c.servicio.precio },
                        pedidosPendientes = listaPedidos.count { p -> p.estado == "PENDIENTE" || p.estado == "LISTO_PARA_RECOGER" },
                        ventasMes = reporteVentas?.totalVentas ?: 0.0,
                        pedidosMes = reporteVentas?.totalPedidos ?: 0,
                        semana = valoresSemana,
                        stockBajo = listaProductos.filter { p -> p.activo && p.stock <= STOCK_BAJO }.sortedBy { p -> p.stock },
                        cargando = false,
                        refrescando = false,
                        cargado = reporteHoy is ApiResult.Success || it.cargado,
                        error = (reporteHoy as? ApiResult.Error)?.message,
                    )
                }
            }
        }
    }
}

@Composable
fun PanelScreen(
    onNuevoProducto: () -> Unit,
    onNuevoVeterinario: () -> Unit,
    onServicios: () -> Unit,
    onCitas: () -> Unit,
    onPedidos: () -> Unit,
    onProductos: () -> Unit,
    onReportes: () -> Unit,
) {
    val vm: PanelViewModel = viewModel(factory = appFactory { PanelViewModel(it.admin, it.citas, it.tienda) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    OnResume { vm.cargar() }

    AppScaffold(title = "Panel") { padding ->
        when {
            ui.cargando && !ui.cargado -> LoadingBox(Modifier.padding(padding))
            ui.error != null && !ui.cargado -> ErrorBox(ui.error!!, { vm.cargar() }, Modifier.padding(padding))
            else -> PullToRefreshBox(
                isRefreshing = ui.refrescando,
                onRefresh = { vm.cargar(mostrarIndicador = true) },
                modifier = Modifier.padding(padding).fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricaCard("Citas hoy", "${ui.citasHoy}", "${ui.atendidasHoy} atendidas", Modifier.weight(1f))
                            MetricaCard(
                                "Por cobrar",
                                "${ui.porCobrar}",
                                ui.montoPorCobrar.lps(),
                                Modifier.weight(1f),
                                destacado = ui.porCobrar > 0,
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricaCard("Pedidos por despachar", "${ui.pedidosPendientes}", null, Modifier.weight(1f))
                            MetricaCard("Ventas del mes", ui.ventasMes.lps(), "${ui.pedidosMes} pedidos", Modifier.weight(1f))
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Citas de la semana", style = MaterialTheme.typography.titleMedium)
                                BarraSemanal(ui.semana, Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccesoRapido(Icons.Default.Add, "Producto", onNuevoProducto, Modifier.weight(1f))
                            AccesoRapido(Icons.Default.Face, "Veterinario", onNuevoVeterinario, Modifier.weight(1f))
                            AccesoRapido(Icons.Default.Settings, "Servicios", onServicios, Modifier.weight(1f))
                        }
                    }
                    item { Seccion("Accesos") }
                    item { FilaNavegable("Citas", "Pendientes, en consulta y cobros", Icons.Default.DateRange, onCitas) }
                    item { FilaNavegable("Pedidos", "Despacho y cobro de pedidos", Icons.AutoMirrored.Filled.List, onPedidos) }
                    item { FilaNavegable("Reportes", "Ventas y citas por fechas", Icons.Default.Info, onReportes) }
                    if (ui.stockBajo.isNotEmpty()) {
                        item { Seccion("Stock bajo") }
                        items(ui.stockBajo, key = { it.id }) { p ->
                            FilaNavegable(
                                p.nombre,
                                if (p.stock == 0) "Agotado" else "Quedan ${p.stock}",
                                Icons.Default.Warning,
                                onProductos,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasScreen(
    onServicios: () -> Unit,
    onVeterinarios: () -> Unit,
    onMascotas: () -> Unit,
    onProductos: () -> Unit,
    onReportes: () -> Unit,
    onPerfil: () -> Unit,
    onClinica: () -> Unit,
) {
    AppScaffold(title = "Más opciones") { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { FilaNavegable("Productos", "Inventario de la tienda", Icons.Default.ShoppingCart, onProductos) }
            item { FilaNavegable("Servicios", "Precios y duración", Icons.Default.Settings, onServicios) }
            item { FilaNavegable("Veterinarios", "Personal y horarios", Icons.Default.Face, onVeterinarios) }
            item { FilaNavegable("Mascotas", "Pacientes registrados", Icons.Default.Favorite, onMascotas) }
            item { FilaNavegable("Reportes", "Ventas y citas", Icons.Default.Info, onReportes) }
            item { FilaNavegable("Datos de la veterinaria", "Nombre, dirección, teléfono y horario", Icons.Default.Place, onClinica) }
            item { FilaNavegable("Perfil", "Tu cuenta y notificaciones", Icons.Default.Person, onPerfil) }
        }
    }
}
