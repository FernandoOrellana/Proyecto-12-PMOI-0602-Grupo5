package com.uth.proyectofinal.ui.dueno

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.AplicacionVacuna
import com.uth.proyectofinal.data.remote.dto.Carne
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.data.remote.dto.Consulta
import com.uth.proyectofinal.data.remote.dto.Historial
import com.uth.proyectofinal.data.remote.dto.ProximaDosis
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.EmptyBox
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.SectionTitle
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.textoFecha
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CarneUi(
    val carne: Carne? = null,
    val historial: Historial? = null,
    val cargando: Boolean = true,
    val error: String? = null,
)

class CarneViewModel(private val id: Int, private val mascotas: MascotaRepository) : ViewModel() {

    private val _ui = MutableStateFlow(CarneUi())
    val ui: StateFlow<CarneUi> = _ui.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            val carne = mascotas.carne(id)
            val historial = mascotas.historial(id)
            val error = (carne as? ApiResult.Error)?.message ?: (historial as? ApiResult.Error)?.message
            _ui.update {
                it.copy(
                    carne = (carne as? ApiResult.Success)?.data,
                    historial = (historial as? ApiResult.Success)?.data,
                    cargando = false,
                    error = error,
                )
            }
        }
    }
}

@Composable
fun CarneHistorialScreen(id: Int, onBack: () -> Unit) {
    val vm: CarneViewModel = viewModel(factory = appFactory { CarneViewModel(id, it.mascotas) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var generando by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    OnResume { vm.cargar() }

    AppScaffold(
        title = "Carné e historial",
        onBack = onBack,
        actions = {
            if (generando) {
                CircularProgressIndicator(Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
            } else if (ui.carne != null || ui.historial != null) {
                IconButton(onClick = {
                    generando = true
                    scope.launch {
                        runCatching {
                            val nombre = ui.carne?.mascota?.nombre?.ifBlank { null }
                                ?: ui.historial?.mascota?.nombre?.ifBlank { null }
                                ?: "Mascota"
                            context.compartirPdf(context.generarCarnePdf(nombre, ui.carne, ui.historial))
                        }.onFailure {
                            Toast.makeText(context, "No se pudo generar el PDF del carné.", Toast.LENGTH_LONG).show()
                        }
                        generando = false
                    }
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir carné en PDF")
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Carné") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Historial") })
            }
            when {
                ui.cargando && ui.carne == null && ui.historial == null -> LoadingBox()
                ui.carne == null && ui.historial == null -> ErrorBox(ui.error ?: "Sin información.", vm::cargar)
                tab == 0 -> CarneList(ui.carne)
                else -> HistorialList(ui.historial)
            }
        }
    }
}

@Composable
private fun CarneList(carne: Carne?) {
    if (carne == null) {
        EmptyBox("Sin información de vacunas.")
        return
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item { SectionTitle("Vacunas aplicadas") }
        if (carne.vacunas.isEmpty()) item { Text("Aún no tiene vacunas registradas.") }
        items(carne.vacunas) { VacunaCard(it) }
        item { SectionTitle("Próximas dosis") }
        if (carne.proximasDosis.isEmpty()) item { Text("No hay dosis pendientes.") }
        items(carne.proximasDosis) { d -> ProximaDosisCard(d) }
    }
}

@Composable
private fun ProximaDosisCard(d: ProximaDosis) {
    val estado = estadoDosis(d.fecha)
    val (fondo, texto) = when (estado.nivel) {
        NivelDosis.VENCIDA -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        NivelDosis.PROXIMA -> Color(0xFFFFE0B2) to Color(0xFF7A4100)
        NivelDosis.PROGRAMADA -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        NivelDosis.DESCONOCIDA -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(d.vacuna.nombre, style = MaterialTheme.typography.titleSmall)
                Text("Fecha: ${d.fecha}")
            }
            Surface(color = fondo, contentColor = texto, shape = RoundedCornerShape(50)) {
                Text(
                    estado.texto,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HistorialList(historial: Historial?) {
    if (historial == null) {
        EmptyBox("Sin historial disponible.")
        return
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        item { SectionTitle("Consultas") }
        if (historial.consultas.isEmpty()) item { Text("Sin consultas registradas.") }
        items(historial.consultas) { ConsultaCard(it) }
        item { SectionTitle("Vacunas") }
        if (historial.vacunas.isEmpty()) item { Text("Sin vacunas registradas.") }
        items(historial.vacunas) { VacunaCard(it) }
        item { SectionTitle("Citas") }
        if (historial.citas.isEmpty()) item { Text("Sin citas.") }
        items(historial.citas) { CitaCard(it) }
    }
}

@Composable
private fun VacunaCard(v: AplicacionVacuna) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(v.vacuna.nombre, style = MaterialTheme.typography.titleSmall)
            Text("Aplicada: ${v.fechaAplicacion} · Vet: ${v.veterinario.nombre}")
            Text("Próxima dosis: ${v.proximaDosis ?: "No programada"}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConsultaCard(c: Consulta) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(c.fecha.textoFecha(), style = MaterialTheme.typography.labelMedium)
            Text("Diagnóstico: ${c.diagnostico}", style = MaterialTheme.typography.titleSmall)
            Text("Tratamiento: ${c.tratamiento ?: "—"}")
            Text("Veterinario: ${c.veterinario.nombre}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CitaCard(c: Cita) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${c.fecha} ${c.hora} · ${c.servicio.nombre}", style = MaterialTheme.typography.titleSmall)
                EstadoBadge(c.estado)
            }
            Text("Vet: ${c.veterinario.nombre}", style = MaterialTheme.typography.bodySmall)
            c.consulta?.let { consulta ->
                Text("Diagnóstico: ${consulta.diagnostico}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
