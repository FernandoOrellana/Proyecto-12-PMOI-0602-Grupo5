package com.uth.proyectofinal.ui.dueno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.CitaRequest
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CatalogRepository
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.data.repository.dataOrNull
import com.uth.proyectofinal.ui.common.ActionButton
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.ErrorText
import com.uth.proyectofinal.ui.common.Option
import com.uth.proyectofinal.ui.common.SelectField
import com.uth.proyectofinal.ui.common.aFechaIso
import com.uth.proyectofinal.ui.common.aMillisUtc
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.esFecha
import com.uth.proyectofinal.ui.common.isoFecha
import com.uth.proyectofinal.ui.common.lps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import com.uth.proyectofinal.ui.common.Avatar
import com.uth.proyectofinal.ui.common.CirculoIcono
import com.uth.proyectofinal.ui.common.DiasSelector
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.OpcionCard
import com.uth.proyectofinal.ui.common.PasosBarra
import com.uth.proyectofinal.ui.common.SectionTitle
import com.uth.proyectofinal.ui.common.fechaCorta
import com.uth.proyectofinal.ui.common.textoCuando
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.ui.common.datosClinica
import com.uth.proyectofinal.ui.common.lugar
import com.uth.proyectofinal.ui.common.agregarAlCalendario
import androidx.compose.material3.SelectableDates
import com.uth.proyectofinal.data.remote.dto.Veterinario
import com.uth.proyectofinal.ui.common.diaSemanaDe
import com.uth.proyectofinal.ui.common.diasHasta
import com.uth.proyectofinal.ui.common.primerDiaLaboral
import androidx.compose.material3.ExperimentalMaterial3Api

data class AgendarUi(
    val mascotas: List<Option> = emptyList(),
    val servicios: List<Option> = emptyList(),
    val veterinarios: List<Option> = emptyList(),
    val mascotaId: Int? = null,
    val servicioId: Int? = null,
    val veterinarioId: Int? = null,
    val fecha: String = isoFecha(),
    val horas: List<String> = emptyList(),
    val hora: String? = null,
    val cargando: Boolean = true,
    val cargandoHoras: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val listo: Boolean = false,
    val creada: Cita? = null,
    val diasLaborales: Set<Int> = DIAS_HABILES,
)

private val DIAS_HABILES = setOf(1, 2, 3, 4, 5)

class AgendarCitaViewModel(
    private val citas: CitaRepository,
    private val mascotas: MascotaRepository,
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(AgendarUi())
    val ui: StateFlow<AgendarUi> = _ui.asStateFlow()

    private var vets: List<Veterinario> = emptyList()

    init {
        cargarCatalogos()
    }

    private fun cargarCatalogos() {
        viewModelScope.launch {
            val mascotasRes = mascotas.list(1, 100)
            val serviciosRes = catalog.servicios()
            val vetsRes = catalog.veterinarios()
            vets = vetsRes.dataOrNull().orEmpty()
            val error = listOf(mascotasRes, serviciosRes, vetsRes)
                .filterIsInstance<ApiResult.Error>()
                .firstOrNull()?.message
            _ui.update {
                it.copy(
                    cargando = false,
                    error = error,
                    mascotas = mascotasRes.dataOrNull()?.data.orEmpty().map { m -> Option(m.id, m.nombre) },
                    servicios = serviciosRes.dataOrNull().orEmpty().map { s ->
                        Option(s.id, "${s.nombre} · ${s.precio.lps()}")
                    },
                    veterinarios = vetsRes.dataOrNull().orEmpty().map { v -> Option(v.id, v.nombre) },
                    fecha = primerDiaLaboral(DIAS_HABILES),
                )
            }
        }
    }

    fun onMascota(id: Int) = _ui.update { it.copy(mascotaId = id, error = null) }

    fun onServicio(id: Int) = _ui.update { it.copy(servicioId = id, error = null) }

    fun onVeterinario(id: Int) {
        val dias = vets.firstOrNull { it.id == id }?.horarios?.map { it.diaSemana }?.toSet()
            ?.takeIf { it.isNotEmpty() } ?: DIAS_HABILES
        _ui.update {
            val fecha = if (diaSemanaDe(it.fecha) in dias) it.fecha else primerDiaLaboral(dias)
            it.copy(veterinarioId = id, diasLaborales = dias, fecha = fecha, error = null)
        }
        refrescarHoras()
    }

    fun onFecha(value: String) {
        _ui.update { it.copy(fecha = value, error = null) }
        refrescarHoras()
    }

    fun onHora(value: String) = _ui.update { it.copy(hora = value, error = null) }

    private fun refrescarHoras() {
        val s = _ui.value
        val veterinario = s.veterinarioId
        if (veterinario == null || !s.fecha.esFecha()) {
            _ui.update { it.copy(horas = emptyList(), hora = null) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(cargandoHoras = true, hora = null, horas = emptyList()) }
            val result = catalog.disponibilidad(veterinario, s.fecha)
            _ui.update {
                it.copy(
                    cargandoHoras = false,
                    horas = result.dataOrNull().orEmpty(),
                    error = (result as? ApiResult.Error)?.message,
                )
            }
        }
    }

    fun confirmar() {
        val s = _ui.value
        val body = if (
            s.mascotaId != null && s.servicioId != null && s.veterinarioId != null &&
            s.hora != null && s.fecha.esFecha()
        ) {
            CitaRequest(s.mascotaId, s.servicioId, s.veterinarioId, s.fecha, s.hora)
        } else {
            null
        }
        if (body == null) {
            _ui.update { it.copy(error = "Completa mascota, servicio, veterinario, fecha y hora.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(guardando = true, error = null) }
            when (val result = citas.crear(body)) {
                is ApiResult.Success -> _ui.update { it.copy(guardando = false, listo = true, creada = result.data) }
                is ApiResult.Error -> _ui.update { it.copy(guardando = false, error = result.message) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendarCitaScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    val vm: AgendarCitaViewModel = viewModel(
        factory = appFactory { AgendarCitaViewModel(it.citas, it.mascotas, it.catalog) },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    var paso by rememberSaveable { mutableIntStateOf(0) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clinica = datosClinica()
    BackHandler(enabled = paso > 0) { paso-- }

    val titulos = listOf("¿Para quién es la cita?", "Elige fecha y hora", "Confirma tu cita")
    val puedeSeguir = when (paso) {
        0 -> ui.mascotaId != null && ui.servicioId != null && ui.veterinarioId != null
        1 -> ui.hora != null
        else -> true
    }

    AppScaffold(title = "Agendar cita", onBack = { if (paso > 0) paso-- else onBack() }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PasosBarra(paso, titulos.size)
                Text(
                    "Paso ${paso + 1} de ${titulos.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(titulos[paso], style = MaterialTheme.typography.titleLarge)
            }
            if (ui.cargando) {
                LoadingBox(Modifier.weight(1f))
            } else {
                AnimatedContent(
                    targetState = paso,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val direccion = if (targetState > initialState) 1 else -1
                        (slideInHorizontally { it / 5 * direccion } + fadeIn()) togetherWith fadeOut()
                    },
                    label = "paso",
                ) { actual ->
                    when (actual) {
                        0 -> PasoSeleccion(ui, vm::onMascota, vm::onServicio, vm::onVeterinario)
                        1 -> PasoHorario(ui, vm::onFecha, vm::onHora) { mostrarCalendario = true }
                        else -> PasoResumen(ui)
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ErrorText(ui.error)
                if (paso < titulos.lastIndex) {
                    Button(
                        onClick = { paso++ },
                        enabled = puedeSeguir,
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Continuar") }
                } else {
                    ActionButton("Confirmar cita", ui.guardando, vm::confirmar)
                }
            }
        }
    }

    if (ui.listo) {
        val cita = ui.creada
        AlertDialog(
            onDismissRequest = onCreated,
            icon = { CheckAnimado() },
            title = { Text("¡Cita agendada!") },
            text = {
                Text(
                    if (cita != null) {
                        "${cita.mascota.nombre} · ${cita.servicio.nombre}\n${textoCuando(cita.fecha)} ${fechaCorta(cita.fecha)} a las ${cita.hora}\n${cita.veterinario.nombre}"
                    } else {
                        "Tu cita quedó registrada."
                    },
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                if (cita != null) {
                    Button(
                        onClick = {
                            context.agregarAlCalendario(
                                titulo = "Cita de ${cita.mascota.nombre} en ${clinica.nombre}",
                                descripcion = "${cita.servicio.nombre} con ${cita.veterinario.nombre}",
                                fecha = cita.fecha,
                                hora = cita.hora,
                                lugar = clinica.lugar(),
                            )
                            onCreated()
                        },
                        shape = MaterialTheme.shapes.small,
                    ) { Text("Agregar a mi calendario") }
                }
            },
            dismissButton = { TextButton(onClick = onCreated) { Text("Listo") } },
        )
    }

    if (mostrarCalendario) {
        val permitidos = ui.diasLaborales
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = ui.fecha.aMillisUtc(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val fecha = utcTimeMillis.aFechaIso()
                    return (diasHasta(fecha) ?: -1) >= 0 && diaSemanaDe(fecha) in permitidos
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { vm.onFecha(it.aFechaIso()) }
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = picker, title = { Text("Elige la fecha de la cita") })
        }
    }
}

@Composable
private fun PasoSeleccion(
    ui: AgendarUi,
    onMascota: (Int) -> Unit,
    onServicio: (Int) -> Unit,
    onVeterinario: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionTitle("Mascota")
        if (ui.mascotas.isEmpty()) {
            Text("Primero registra una mascota.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ui.mascotas.forEach { m ->
            OpcionCard(m.label, null, ui.mascotaId == m.id, { onMascota(m.id) }) { Avatar(m.label) }
        }
        SectionTitle("Servicio")
        ui.servicios.forEach { s ->
            OpcionCard(
                s.label.substringBefore(" · "),
                s.label.substringAfter(" · ", "").ifEmpty { null },
                ui.servicioId == s.id,
                { onServicio(s.id) },
            ) { CirculoIcono(Icons.Default.Favorite) }
        }
        SectionTitle("Veterinario")
        ui.veterinarios.forEach { v ->
            OpcionCard(v.label, null, ui.veterinarioId == v.id, { onVeterinario(v.id) }) { CirculoIcono(Icons.Default.Person) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PasoHorario(
    ui: AgendarUi,
    onFecha: (String) -> Unit,
    onHora: (String) -> Unit,
    onOtraFecha: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        DiasSelector(
            fecha = ui.fecha,
            onFecha = onFecha,
            onOtraFecha = onOtraFecha,
            dias = 28,
            diasPermitidos = ui.diasLaborales,
        )
        Text(
            "Solo se muestran los días que atiende el veterinario.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "${textoCuando(ui.fecha)} · ${fechaCorta(ui.fecha)}",
                style = MaterialTheme.typography.titleMedium,
            )
            when {
                ui.cargandoHoras -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                ui.horas.isEmpty() -> Text(
                    "No hay horarios disponibles ese día. Prueba con otra fecha.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val (manana, tarde) = ui.horas.partition { it < "12:00" }
                    listOf("Mañana" to manana, "Tarde" to tarde).filter { it.second.isNotEmpty() }.forEach { (titulo, horas) ->
                        SectionTitle(titulo)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            horas.forEach { h ->
                                FilterChip(
                                    selected = ui.hora == h,
                                    onClick = { onHora(h) },
                                    label = { Text(h) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasoResumen(ui: AgendarUi) {
    val mascota = ui.mascotas.firstOrNull { it.id == ui.mascotaId }?.label.orEmpty()
    val servicio = ui.servicios.firstOrNull { it.id == ui.servicioId }?.label.orEmpty()
    val veterinario = ui.veterinarios.firstOrNull { it.id == ui.veterinarioId }?.label.orEmpty()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(mascota, Modifier.size(56.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(mascota, style = MaterialTheme.typography.titleLarge)
                        Text(
                            servicio.substringBefore(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                ResumenFila(Icons.Default.DateRange, "Fecha", "${textoCuando(ui.fecha)} · ${fechaCorta(ui.fecha)}")
                ResumenFila(Icons.Default.Notifications, "Hora", ui.hora.orEmpty())
                ResumenFila(Icons.Default.Person, "Veterinario", veterinario)
                ResumenFila(Icons.Default.ShoppingCart, "Precio", servicio.substringAfter(" · ", "—"))
            }
        }
    }
}

@Composable
private fun ResumenFila(icon: ImageVector, titulo: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CirculoIcono(icon, 36.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CheckAnimado() {
    val escala = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        Modifier
            .size(64.dp)
            .graphicsLayer {
                scaleX = escala.value
                scaleY = escala.value
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
    }
}
