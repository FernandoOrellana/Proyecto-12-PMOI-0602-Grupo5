package com.uth.proyectofinal.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.ReporteCitas
import com.uth.proyectofinal.data.remote.dto.ReporteVentas
import com.uth.proyectofinal.data.repository.AdminRepository
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.ui.common.ActionButton
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.BarraActualizando
import com.uth.proyectofinal.ui.common.AppTextField
import com.uth.proyectofinal.ui.common.ErrorText
import com.uth.proyectofinal.ui.common.SectionTitle
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.esFecha
import com.uth.proyectofinal.ui.common.isoFecha
import com.uth.proyectofinal.ui.common.lps
import com.uth.proyectofinal.ui.common.primerDiaDelMes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.FiltroChips
import com.uth.proyectofinal.ui.common.MetricaCard
import com.uth.proyectofinal.ui.common.aFechaIso
import com.uth.proyectofinal.ui.common.aMillisUtc
import com.uth.proyectofinal.ui.common.fechaCorta
import com.uth.proyectofinal.ui.common.fechaMasDias

data class ReportesUi(
    val desde: String = primerDiaDelMes(),
    val hasta: String = isoFecha(),
    val ventas: ReporteVentas? = null,
    val citas: ReporteCitas? = null,
    val cargando: Boolean = false,
    val error: String? = null,
)

class ReportesViewModel(private val admin: AdminRepository) : ViewModel() {

    private val _ui = MutableStateFlow(ReportesUi())
    val ui: StateFlow<ReportesUi> = _ui.asStateFlow()

    private var trabajo: Job? = null

    fun onDesde(value: String) {
        _ui.update { it.copy(desde = value, error = null) }
        generar()
    }

    fun onHasta(value: String) {
        _ui.update { it.copy(hasta = value, error = null) }
        generar()
    }

    fun rango(desde: String, hasta: String) {
        _ui.update { it.copy(desde = desde, hasta = hasta, error = null) }
        generar()
    }

    fun generar() {
        val s = _ui.value
        if (!s.desde.esFecha() || !s.hasta.esFecha()) {
            _ui.update { it.copy(error = "Las fechas deben tener formato AAAA-MM-DD.") }
            return
        }
        if (s.desde > s.hasta) {
            _ui.update { it.copy(error = "La fecha inicial no puede ser mayor que la final.", ventas = null, citas = null) }
            return
        }
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            val ventas = admin.reporteVentas(s.desde, s.hasta)
            val citas = admin.reporteCitas(s.desde, s.hasta)
            val error = (ventas as? ApiResult.Error)?.message ?: (citas as? ApiResult.Error)?.message
            _ui.update {
                it.copy(
                    cargando = false,
                    ventas = (ventas as? ApiResult.Success)?.data,
                    citas = (citas as? ApiResult.Success)?.data,
                    error = error,
                )
            }
        }
    }
}

private enum class Rango(val texto: String) { HOY("Hoy"), SEMANA("7 días"), MES("Este mes"), ANIO("Este año") }

private fun fechasDe(rango: Rango): Pair<String, String> {
    val hoy = isoFecha()
    return when (rango) {
        Rango.HOY -> hoy to hoy
        Rango.SEMANA -> fechaMasDias(-6) to hoy
        Rango.MES -> primerDiaDelMes() to hoy
        Rango.ANIO -> "${hoy.take(4)}-01-01" to hoy
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen() {
    val vm: ReportesViewModel = viewModel(factory = appFactory { ReportesViewModel(it.admin) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportando by remember { mutableStateOf(false) }
    var eligiendo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (ui.ventas == null && ui.citas == null) vm.generar()
    }

    val rangoActual = Rango.entries.firstOrNull { fechasDe(it) == (ui.desde to ui.hasta) }
    val hayDatos = ui.ventas != null || ui.citas != null

    AppScaffold(
        title = "Reportes",
        actions = {
            if (hayDatos) {
                IconButton(
                    enabled = !exportando && !ui.cargando,
                    onClick = {
                        exportando = true
                        scope.launch {
                            try {
                                val archivo = context.generarReportePdf(ui.desde, ui.hasta, ui.ventas, ui.citas)
                                context.compartirReporte(archivo)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo generar el PDF.", Toast.LENGTH_SHORT).show()
                            } finally {
                                exportando = false
                            }
                        }
                    },
                ) {
                    if (exportando) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Share, contentDescription = "Exportar PDF")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FiltroChips(
                opciones = Rango.entries.map { it.texto },
                seleccion = rangoActual?.texto.orEmpty(),
                onSelect = { texto ->
                    Rango.entries.firstOrNull { it.texto == texto }?.let { r ->
                        val (d, h) = fechasDe(r)
                        vm.rango(d, h)
                    }
                },
            )
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FechaBoton("Desde", ui.desde, Modifier.weight(1f)) { eligiendo = "desde" }
                    FechaBoton("Hasta", ui.hasta, Modifier.weight(1f)) { eligiendo = "hasta" }
                }
                ErrorText(ui.error)
                BarraActualizando(ui.cargando)

                ui.ventas?.let { v ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricaCard("Total vendido", v.totalVentas.lps(), "${v.totalPedidos} pedidos", Modifier.weight(1f))
                        MetricaCard("Citas", "${ui.citas?.total ?: 0}", rangoActual?.texto ?: "Periodo elegido", Modifier.weight(1f))
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Productos más vendidos", style = MaterialTheme.typography.titleMedium)
                            val productos = v.productos.sortedByDescending { it.total }
                            if (productos.isEmpty()) {
                                Text("Sin ventas en el periodo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val maximo = productos.maxOfOrNull { it.total } ?: 1.0
                            productos.take(8).forEach { p ->
                                BarraHorizontal(p.nombre, (p.total / maximo).toFloat(), "${p.cantidad} · ${p.total.lps()}")
                            }
                        }
                    }
                }

                ui.citas?.let { c ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Citas por servicio", style = MaterialTheme.typography.titleMedium)
                            if (c.porServicio.isEmpty()) {
                                Text("Sin citas en el periodo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val maximo = (c.porServicio.maxOfOrNull { it.cantidad } ?: 1).coerceAtLeast(1)
                            c.porServicio.sortedByDescending { it.cantidad }.forEach { s ->
                                BarraHorizontal(s.nombre, s.cantidad.toFloat() / maximo, "${s.cantidad}")
                            }
                        }
                    }
                    if (c.porEstado.isNotEmpty()) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Citas por estado", style = MaterialTheme.typography.titleMedium)
                                c.porEstado.forEach { (estado, cantidad) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.weight(1f)) { EstadoBadge(estado) }
                                        Text("$cantidad", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    eligiendo?.let { campo ->
        val actual = if (campo == "desde") ui.desde else ui.hasta
        val picker = rememberDatePickerState(initialSelectedDateMillis = actual.aMillisUtc())
        DatePickerDialog(
            onDismissRequest = { eligiendo = null },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        if (campo == "desde") vm.onDesde(it.aFechaIso()) else vm.onHasta(it.aFechaIso())
                    }
                    eligiendo = null
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { eligiendo = null }) { Text("Cancelar") } },
        ) {
            DatePicker(state = picker, title = { Text(if (campo == "desde") "Fecha inicial" else "Fecha final") })
        }
    }
}

@Composable
private fun FechaBoton(titulo: String, fecha: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fechaCorta(fecha), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun BarraHorizontal(nombre: String, fraccion: Float, valor: String) {
    val animada by animateFloatAsState(fraccion.coerceIn(0f, 1f), tween(700), label = "barra")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                nombre,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(valor, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animada)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
