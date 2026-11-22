package com.uth.proyectofinal.ui.mascota

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.data.remote.dto.Consulta
import com.uth.proyectofinal.data.remote.dto.Mascota
import com.uth.proyectofinal.data.remote.dto.Ref
import com.uth.proyectofinal.data.remote.dto.Rol
import com.uth.proyectofinal.data.remote.dto.ServicioResumen
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.EmptyBox
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.EstadoBadge
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.SectionTitle
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.lps
import com.uth.proyectofinal.ui.common.textoFecha
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistorialCitasUi(
    val mascota: Mascota? = null,
    val citas: List<Cita> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
    val guardandoObservacion: Boolean = false,
)

class HistorialCitasViewModel(
    private val mascotaId: Int,
    private val repo: MascotaRepository,
    private val citasRepo: CitaRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(HistorialCitasUi())
    val ui: StateFlow<HistorialCitasUi> = _ui.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            val resMascota = repo.get(mascotaId)
            val resCitas = repo.citas(mascotaId)

            val err = (resMascota as? ApiResult.Error)?.message
                ?: (resCitas as? ApiResult.Error)?.message

            _ui.update {
                it.copy(
                    mascota = (resMascota as? ApiResult.Success)?.data,
                    citas = (resCitas as? ApiResult.Success)?.data ?: emptyList(),
                    cargando = false,
                    error = err,
                )
            }
        }
    }

    fun guardarObservacion(citaId: Int, observaciones: String, onExito: () -> Unit) {
        viewModelScope.launch {
            _ui.update { it.copy(guardandoObservacion = true, error = null) }
            when (val res = citasRepo.agregarObservacion(citaId, observaciones)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(guardandoObservacion = false) }
                    onExito()
                    cargar()
                }
                is ApiResult.Error -> {
                    _ui.update { it.copy(guardandoObservacion = false, error = res.message) }
                }
            }
        }
    }
}

@Composable
fun HistorialCitasMascotaScreen(
    mascotaId: Int,
    rol: String? = null,
    onBack: () -> Unit,
) {
    val vm: HistorialCitasViewModel = viewModel(
        factory = appFactory { HistorialCitasViewModel(mascotaId, it.mascotas, it.citas) },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()

    OnResume { vm.cargar() }

    HistorialCitasMascotaContent(
        ui = ui,
        rol = rol,
        onBack = onBack,
        onRetry = vm::cargar,
        onGuardarObservacion = { citaId, obs, onExito ->
            vm.guardarObservacion(citaId, obs, onExito)
        },
    )
}

@Composable
fun HistorialCitasMascotaContent(
    ui: HistorialCitasUi,
    rol: String? = null,
    onBack: () -> Unit = {},
    onRetry: () -> Unit = {},
    onGuardarObservacion: (Int, String, () -> Unit) -> Unit = { _, _, _ -> },
) {
    val puedeEditarObservaciones = (rol == Rol.VETERINARIO || rol == Rol.ADMINISTRADOR)

    var citaParaObservacion by remember { mutableStateOf<Cita?>(null) }
    var textoObservacion by remember { mutableStateOf("") }

    AppScaffold(
        title = "Historial de citas",
        onBack = onBack,
    ) { padding ->
        when {
            ui.cargando -> LoadingBox(Modifier.padding(padding))
            ui.error != null && ui.mascota == null -> ErrorBox(
                ui.error ?: "Error al cargar el historial.",
                onRetry,
                Modifier.padding(padding),
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ui.mascota?.let { m ->
                        item {
                            MascotaHeaderCard(m)
                        }
                    }

                    item {
                        SectionTitle("Registro histórico de citas (${ui.citas.size})")
                    }

                    if (ui.citas.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Text(
                                    text = "Esta mascota aún no cuenta con citas registradas en el historial.",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else {
                        items(ui.citas, key = { it.id }) { cita ->
                            CitaHistorialCard(
                                cita = cita,
                                puedeEditarObservacion = puedeEditarObservaciones,
                                onAgregarObservacion = {
                                    citaParaObservacion = it
                                    textoObservacion = it.consulta?.observaciones.orEmpty()
                                },
                            )
                        }

                        if (puedeEditarObservaciones) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            text = "💡 Puedes agregar o editar observaciones clínicas en cualquier cita del historial utilizando el botón al final de cada tarjeta.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (citaParaObservacion != null) {
        val cita = citaParaObservacion!!
        val tieneObs = !cita.consulta?.observaciones.isNullOrBlank()
        AlertDialog(
            onDismissRequest = { if (!ui.guardandoObservacion) citaParaObservacion = null },
            title = {
                Text(
                    text = if (tieneObs) "Editar observación" else "Agregar observación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cita del ${cita.fecha.textoFecha()} (${cita.hora}) - ${cita.servicio.nombre}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    OutlinedTextField(
                        value = textoObservacion,
                        onValueChange = { textoObservacion = it },
                        label = { Text("Observación del veterinario") },
                        placeholder = { Text("Escribe notas clínicas, recomendaciones o evolución...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        enabled = !ui.guardandoObservacion,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textoObservacion.isNotBlank()) {
                            onGuardarObservacion(cita.id, textoObservacion.trim()) {
                                citaParaObservacion = null
                            }
                        }
                    },
                    enabled = !ui.guardandoObservacion && textoObservacion.isNotBlank(),
                ) {
                    if (ui.guardandoObservacion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Guardar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { citaParaObservacion = null },
                    enabled = !ui.guardandoObservacion,
                ) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun MascotaHeaderCard(m: Mascota) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = m.nombre,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            val infoEspecieRaza = listOfNotNull(m.especie?.nombre, m.raza).joinToString(" · ")
            if (infoEspecieRaza.isNotBlank()) {
                Text(
                    text = infoEspecieRaza,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            m.pesoKg?.let { peso ->
                Text(
                    text = "Peso registrado: $peso kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun CitaHistorialCard(
    cita: Cita,
    puedeEditarObservacion: Boolean,
    onAgregarObservacion: (Cita) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = cita.fecha.textoFecha(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Hora: ${cita.hora}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                EstadoBadge(cita.estado)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Servicio: ${cita.servicio.nombre} (${cita.servicio.precio.lps()})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text = "Veterinario: ${cita.veterinario.nombre}",
                style = MaterialTheme.typography.bodyMedium,
            )

            cita.dueno?.let { dueno ->
                Text(
                    text = "Dueño: ${dueno.nombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            cita.consulta?.let { consulta ->
                ConsultaDetalleBox(consulta)
            }

            if (puedeEditarObservacion) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val tieneObs = !cita.consulta?.observaciones.isNullOrBlank()
                    OutlinedButton(
                        onClick = { onAgregarObservacion(cita) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = if (tieneObs) "✏️ Editar observación" else "+ Agregar observación",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsultaDetalleBox(consulta: Consulta) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Detalle de atención clínica",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Diagnóstico: ${consulta.diagnostico}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            if (!consulta.tratamiento.isNullOrBlank()) {
                Text(
                    text = "Tratamiento: ${consulta.tratamiento}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!consulta.observaciones.isNullOrBlank()) {
                Text(
                    text = "Observaciones: ${consulta.observaciones}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            consulta.peso?.let { peso ->
                Text(
                    text = "Peso al atender: $peso kg",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistorialCitasMascotaScreenPreview() {
    PetCareTheme {
        HistorialCitasMascotaContent(
            ui = HistorialCitasUi(
                mascota = Mascota(
                    id = 1,
                    nombre = "Max",
                    especie = Ref(1, "Perro"),
                    raza = "Golden Retriever",
                    pesoKg = 28.5,
                ),
                citas = listOf(
                    Cita(
                        id = 1,
                        fecha = "2026-03-30",
                        hora = "09:00",
                        estado = "ATENDIDA",
                        servicio = ServicioResumen(1, "Consulta General", 350.0),
                        veterinario = Ref(1, "Dr. Carlos"),
                        dueno = Ref(1, "Juan Pérez"),
                        consulta = Consulta(
                            id = 1,
                            diagnostico = "Sin anomalías detectadas",
                            observaciones = "Mascota en muy buen estado de salud.",
                            peso = 28.5,
                        ),
                    ),
                ),
                cargando = false,
            ),
        )
    }
}
