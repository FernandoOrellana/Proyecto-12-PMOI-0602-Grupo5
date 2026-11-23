package com.uth.proyectofinal.ui.dueno

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.uth.proyectofinal.data.remote.dto.Mascota
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.AuthRepository
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.data.repository.dataOrNull
import com.uth.proyectofinal.ui.common.AccesoRapido
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.FotoCirculo
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.Seccion
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.diasHasta
import com.uth.proyectofinal.ui.common.textoCuando
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import com.uth.proyectofinal.ui.common.CirculoIcono
import com.uth.proyectofinal.ui.common.VeterinariaCard
import com.uth.proyectofinal.ui.common.datosClinica
import com.uth.proyectofinal.ui.common.lugar
import com.uth.proyectofinal.ui.common.abrirMapa
import com.uth.proyectofinal.ui.common.agregarAlCalendario
import com.uth.proyectofinal.ui.common.escribirWhatsApp
import com.uth.proyectofinal.ui.common.llamar

data class AvisoDosis(val mascota: String, val vacuna: String, val fecha: String, val vencida: Boolean)

data class InicioUi(
    val nombre: String = "",
    val proxima: Cita? = null,
    val mascotas: List<Mascota> = emptyList(),
    val avisos: List<AvisoDosis> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val cargado: Boolean = false,
)

class InicioViewModel(
    private val auth: AuthRepository,
    private val citas: CitaRepository,
    private val mascotas: MascotaRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(InicioUi(nombre = auth.user?.nombre.orEmpty()))
    val ui: StateFlow<InicioUi> = _ui.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            val (mascotasRes, citasRes) = coroutineScope {
                val m = async { mascotas.list(1, 20) }
                val c = async { citas.list(null, null, 1, 50) }
                m.await() to c.await()
            }
            val listaMascotas = mascotasRes.dataOrNull()?.data.orEmpty()
            val proxima = citasRes.dataOrNull()?.data.orEmpty()
                .filter { it.estado == CitaEstado.PENDIENTE || it.estado == CitaEstado.CONFIRMADA }
                .filter {
                    val dias = diasHasta(it.fecha) ?: -1
                    dias > 0 || (dias == 0 && it.hora >= horaActual())
                }
                .minByOrNull { "${it.fecha} ${it.hora}" }
            val avisos = coroutineScope {
                listaMascotas.take(5).map { m ->
                    async {
                        mascotas.carne(m.id).dataOrNull()?.proximasDosis.orEmpty().mapNotNull { dosis ->
                            val dias = diasHasta(dosis.fecha) ?: return@mapNotNull null
                            if (dias > 7) null else AvisoDosis(m.nombre, dosis.vacuna.nombre, dosis.fecha, dias < 0)
                        }
                    }
                }.awaitAll().flatten().sortedBy { it.fecha }
            }
            val error = (mascotasRes as? ApiResult.Error)?.message
            _ui.update {
                it.copy(
                    nombre = auth.user?.nombre.orEmpty(),
                    proxima = proxima,
                    mascotas = listaMascotas,
                    avisos = avisos,
                    cargando = false,
                    error = error,
                    cargado = error == null || it.cargado,
                )
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
fun InicioScreen(
    onAgendar: () -> Unit,
    onCitas: () -> Unit,
    onTienda: () -> Unit,
    onPedidos: () -> Unit,
    onMascotas: () -> Unit,
    onMascota: (Int) -> Unit,
    onNuevaMascota: () -> Unit,
    onNotificaciones: () -> Unit,
) {
    val vm: InicioViewModel = viewModel(factory = appFactory { InicioViewModel(it.auth, it.citas, it.mascotas) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    OnResume { vm.cargar() }

    val primerNombre = ui.nombre.trim().substringBefore(' ')
    AppScaffold(
        title = if (primerNombre.isBlank()) saludo() else "${saludo()}, $primerNombre",
        actions = {
            IconButton(onClick = onNotificaciones) {
                Icon(Icons.Default.Notifications, contentDescription = "Notificaciones")
            }
        },
    ) { padding ->
        when {
            ui.cargando && !ui.cargado -> LoadingBox(Modifier.padding(padding))
            ui.error != null && !ui.cargado -> ErrorBox(ui.error!!, vm::cargar, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ProximaCitaCard(ui.proxima, onAgendar, onCitas) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccesoRapido(Icons.Default.Add, "Agendar", onAgendar, Modifier.weight(1f))
                        AccesoRapido(Icons.Default.DateRange, "Mis citas", onCitas, Modifier.weight(1f))
                        AccesoRapido(Icons.Default.ShoppingCart, "Tienda", onTienda, Modifier.weight(1f))
                        AccesoRapido(Icons.AutoMirrored.Filled.List, "Pedidos", onPedidos, Modifier.weight(1f))
                    }
                }
                item { VeterinariaCard() }
                item {
                    Seccion("Mis mascotas") {
                        TextButton(onClick = onMascotas) { Text("Ver todas") }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ui.mascotas, key = { it.id }) { m ->
                            Card(onClick = { onMascota(m.id) }, modifier = Modifier.width(104.dp)) {
                                Column(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    FotoCirculo(m.fotoUrl, m.nombre, 56.dp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(m.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        m.especie?.nombre.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        item {
                            Card(
                                onClick = onNuevaMascota,
                                modifier = Modifier.width(80.dp).height(124.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            ) {
                                Column(
                                    Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar mascota", tint = MaterialTheme.colorScheme.primary)
                                    Text("Nueva", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                if (ui.avisos.isNotEmpty()) {
                    item { Seccion("Recordatorios") }
                    items(ui.avisos) { aviso -> AvisoCard(aviso) }
                }
            }
        }
    }
}

@Composable
private fun ProximaCitaCard(cita: Cita?, onAgendar: () -> Unit, onCitas: () -> Unit) {
    Card(
        onClick = if (cita != null) onCitas else onAgendar,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Próxima cita", style = MaterialTheme.typography.labelLarge)
            if (cita == null) {
                Text("No tienes citas pendientes", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onAgendar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.small,
                ) { Text("Agendar una cita") }
            } else {
                Text("${textoCuando(cita.fecha)} · ${cita.hora}", style = MaterialTheme.typography.headlineSmall)
                Text("${cita.mascota.nombre} · ${cita.servicio.nombre}", style = MaterialTheme.typography.bodyLarge)
                Text(cita.veterinario.nombre, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                val context = LocalContext.current
                val clinica = datosClinica()
                Button(
                    onClick = {
                        context.agregarAlCalendario(
                            titulo = "Cita de ${cita.mascota.nombre} en ${clinica.nombre}",
                            descripcion = "${cita.servicio.nombre} con ${cita.veterinario.nombre}",
                            fecha = cita.fecha,
                            hora = cita.hora,
                            lugar = clinica.lugar(),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar a mi calendario")
                }
            }
        }
    }
}

@Composable
private fun AvisoCard(aviso: AvisoDosis) {
    val fondo = if (aviso.vencida) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer
    val texto = if (aviso.vencida) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fondo, contentColor = texto),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                if (aviso.vencida) {
                    "La vacuna ${aviso.vacuna} de ${aviso.mascota} está vencida."
                } else {
                    "A ${aviso.mascota} le toca ${aviso.vacuna}: ${textoCuando(aviso.fecha).lowercase()}."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun horaActual(): String = SimpleDateFormat("HH:mm", Locale.US).format(Date())
