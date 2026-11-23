@file:OptIn(ExperimentalMaterial3Api::class)

package com.uth.proyectofinal.ui.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.data.remote.dto.CitaEstado
import com.uth.proyectofinal.data.remote.dto.Horario
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.AuthRepository
import com.uth.proyectofinal.data.repository.CatalogRepository
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.dataOrNull
import com.uth.proyectofinal.ui.common.AccesoRapido
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.Avatar
import com.uth.proyectofinal.ui.common.BarraSemanal
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.MetricaCard
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.Seccion
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.fechaMasDias
import com.uth.proyectofinal.ui.common.isoFecha
import com.uth.proyectofinal.ui.common.nombreDia
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class VetInicioUi(
    val nombre: String = "",
    val email: String = "",
    val horarioHoy: Horario? = null,
    val citasHoy: List<Cita> = emptyList(),
    val semana: List<Pair<String, Int>> = emptyList(),
    val cargando: Boolean = true,
    val refrescando: Boolean = false,
    val cargado: Boolean = false,
    val error: String? = null,
) {
    val pendientes: Int get() = citasHoy.count { it.estado == CitaEstado.PENDIENTE || it.estado == CitaEstado.CONFIRMADA }
    val atendidas: Int get() = citasHoy.count { it.estado == CitaEstado.ATENDIDA }
    val enConsulta: Cita? get() = citasHoy.firstOrNull { it.estado == CitaEstado.EN_CONSULTA }
}

class VetInicioViewModel(
    private val auth: AuthRepository,
    private val citas: CitaRepository,
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(VetInicioUi(nombre = auth.user?.nombre.orEmpty(), email = auth.user?.email.orEmpty()))
    val ui: StateFlow<VetInicioUi> = _ui.asStateFlow()

    fun cargar(mostrarIndicador: Boolean = false) {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, refrescando = mostrarIndicador, error = null) }
            val hoy = isoFecha()
            val diaSemana = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
            coroutineScope {
                val citasHoy = async { citas.list(hoy, null, 1, 50) }
                val veterinarios = async { catalog.veterinarios() }
                val semana = (0..6).map { d ->
                    async {
                        val dia = fechaMasDias(d)
                        nombreDia(dia) to (citas.list(dia, null, 1, 50).dataOrNull()?.data
                            ?.count { it.estado != CitaEstado.CANCELADA } ?: 0)
                    }
                }
                val resultadoHoy = citasHoy.await()
                val email = auth.user?.email.orEmpty()
                val yo = veterinarios.await().dataOrNull().orEmpty().firstOrNull { it.email.equals(email, ignoreCase = true) }
                val valores = semana.awaitAll()
                _ui.update {
                    it.copy(
                        nombre = auth.user?.nombre.orEmpty(),
                        email = email,
                        horarioHoy = yo?.horarios?.firstOrNull { h -> h.diaSemana == diaSemana },
                        citasHoy = resultadoHoy.dataOrNull()?.data.orEmpty()
                            .filter { c -> c.estado != CitaEstado.CANCELADA }
                            .sortedBy { c -> c.hora },
                        semana = valores,
                        cargando = false,
                        refrescando = false,
                        cargado = resultadoHoy is ApiResult.Success || it.cargado,
                        error = (resultadoHoy as? ApiResult.Error)?.message,
                    )
                }
            }
        }
    }
}

private fun saludo(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Buenos días"
    in 12..17 -> "Buenas tardes"
    else -> "Buenas noches"
}

@Composable
fun VetInicioScreen(
    onAgenda: () -> Unit,
    onPacientes: () -> Unit,
    onNotificaciones: () -> Unit,
    onOpenCita: (Int) -> Unit,
) {
    val vm: VetInicioViewModel = viewModel(factory = appFactory { VetInicioViewModel(it.auth, it.citas, it.catalog) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    OnResume { vm.cargar() }

    val ahora = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    val siguiente = ui.enConsulta
        ?: ui.citasHoy.firstOrNull { (it.estado == CitaEstado.PENDIENTE || it.estado == CitaEstado.CONFIRMADA) && it.hora >= ahora }

    AppScaffold(
        title = saludo(),
        actions = {
            IconButton(onClick = onNotificaciones) {
                Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
            }
        },
    ) { padding ->
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
                    item { PerfilCard(ui) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricaCard("Citas hoy", "${ui.citasHoy.size}", null, Modifier.weight(1f))
                            MetricaCard("Pendientes", "${ui.pendientes}", null, Modifier.weight(1f), destacado = ui.pendientes > 0)
                            MetricaCard("Atendidas", "${ui.atendidas}", null, Modifier.weight(1f))
                        }
                    }
                    item { SiguienteCard(siguiente, onOpenCita, onAgenda) }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AccesoRapido(Icons.Default.DateRange, "Agenda", onAgenda, Modifier.weight(1f))
                            AccesoRapido(Icons.Default.Favorite, "Pacientes", onPacientes, Modifier.weight(1f))
                            AccesoRapido(Icons.Default.Notifications, "Avisos", onNotificaciones, Modifier.weight(1f))
                        }
                    }
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Tus próximos 7 días", style = MaterialTheme.typography.titleMedium)
                                BarraSemanal(ui.semana, Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                    if (ui.citasHoy.isNotEmpty()) {
                        item { Seccion("Citas de hoy") }
                        items(ui.citasHoy, key = { it.id }) { cita ->
                            Card(onClick = { onOpenCita(cita.id) }, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(cita.hora, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(56.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "${cita.mascota.nombre} · ${cita.servicio.nombre}",
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            cita.dueno?.nombre.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    EstadoBadge(cita.estado)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerfilCard(ui: VetInicioUi) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(ui.nombre, Modifier.size(60.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(ui.nombre, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ui.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                val horario = ui.horarioHoy
                Text(
                    if (horario != null) "Hoy atiendes de ${horario.horaInicio} a ${horario.horaFin}" else "Hoy no tienes horario asignado",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SiguienteCard(cita: Cita?, onOpenCita: (Int) -> Unit, onAgenda: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (cita == null) {
                Text("Siguiente paciente", style = MaterialTheme.typography.labelLarge)
                Text("No tienes más citas hoy", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onAgenda,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Ver agenda") }
            } else {
                Text(
                    if (cita.estado == CitaEstado.EN_CONSULTA) "En consulta ahora" else "Siguiente paciente",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text("${cita.hora} · ${cita.mascota.nombre}", style = MaterialTheme.typography.headlineSmall)
                Text("${cita.servicio.nombre} · ${cita.dueno?.nombre.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { onOpenCita(cita.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.small,
                ) { Text(if (cita.estado == CitaEstado.EN_CONSULTA) "Continuar consulta" else "Abrir cita") }
            }
        }
    }
}
