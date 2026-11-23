package com.uth.proyectofinal.ui.dueno

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.uth.proyectofinal.data.remote.dto.Mascota
import com.uth.proyectofinal.data.remote.dto.Ref
import com.uth.proyectofinal.data.remote.dto.Rol
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.ConfirmDialog
import com.uth.proyectofinal.ui.common.EditIcon
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.FotoSelector
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.MessageViewModel
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.imagenPart
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import com.uth.proyectofinal.data.remote.dto.ProximaDosis
import com.uth.proyectofinal.ui.common.AccesoRapido
import com.uth.proyectofinal.ui.common.Seccion
import com.uth.proyectofinal.ui.common.diasHasta
import com.uth.proyectofinal.ui.common.fechaCorta

data class MascotaDetailUi(
    val mascota: Mascota? = null,
    val cargando: Boolean = true,
    val error: String? = null,
    val ocupado: Boolean = false,
    val eliminada: Boolean = false,
    val proximas: List<ProximaDosis> = emptyList(),
)

class MascotaDetailViewModel(
    private val id: Int,
    private val mascotas: MascotaRepository,
) : MessageViewModel() {

    private val _ui = MutableStateFlow(MascotaDetailUi())
    val ui: StateFlow<MascotaDetailUi> = _ui.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            when (val result = mascotas.get(id)) {
                is ApiResult.Success -> _ui.update { it.copy(mascota = result.data, cargando = false) }
                is ApiResult.Error -> _ui.update { it.copy(cargando = false, error = result.message) }
            }
            val carne = mascotas.carne(id)
            if (carne is ApiResult.Success) {
                _ui.update { it.copy(proximas = carne.data.proximasDosis.sortedBy { d -> d.fecha }) }
            }
        }
    }

    fun subirFoto(foto: MultipartBody.Part) {
        viewModelScope.launch {
            _ui.update { it.copy(ocupado = true) }
            when (val result = mascotas.subirFoto(id, foto)) {
                is ApiResult.Success -> {
                    _ui.update { it.copy(mascota = result.data) }
                    say("Foto actualizada.")
                }
                is ApiResult.Error -> say(result.message)
            }
            _ui.update { it.copy(ocupado = false) }
        }
    }

    fun eliminar() {
        viewModelScope.launch {
            _ui.update { it.copy(ocupado = true) }
            when (val result = mascotas.delete(id)) {
                is ApiResult.Success -> _ui.update { it.copy(eliminada = true) }
                is ApiResult.Error -> {
                    say(result.message)
                    _ui.update { it.copy(ocupado = false) }
                }
            }
        }
    }
}

@Composable
fun MascotaDetailScreen(
    id: Int,
    rol: String? = null,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onCarne: (Int) -> Unit,
    onHistorialCitas: (Int) -> Unit,
    onDeleted: () -> Unit,
    onAgendar: () -> Unit = {},
) {
    val vm: MascotaDetailViewModel = viewModel(factory = appFactory { MascotaDetailViewModel(id, it.mascotas) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    OnResume { vm.cargar() }

    LaunchedEffect(ui.eliminada) {
        if (ui.eliminada) onDeleted()
    }

    MascotaDetailContent(
        id = id,
        ui = ui,
        rol = rol,
        messages = vm.messages,
        onBack = onBack,
        onEdit = onEdit,
        onCarne = onCarne,
        onHistorialCitas = onHistorialCitas,
        onRetry = vm::cargar,
        onFotoSelected = { uri -> scope.launch { vm.subirFoto(context.imagenPart(uri, "foto")) } },
        onEliminar = vm::eliminar,
        onAgendar = onAgendar,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MascotaDetailContent(
    id: Int,
    ui: MascotaDetailUi,
    rol: String? = null,
    messages: Flow<String>? = null,
    onBack: () -> Unit = {},
    onEdit: (Int) -> Unit = {},
    onCarne: (Int) -> Unit = {},
    onHistorialCitas: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onFotoSelected: (Uri) -> Unit = {},
    onEliminar: () -> Unit = {},
    onAgendar: () -> Unit = {},
) {
    var confirmar by remember { mutableStateOf(false) }
    val esDueno = rol == null || rol == Rol.DUENO

    AppScaffold(
        title = ui.mascota?.nombre ?: "Mascota",
        onBack = onBack,
        messages = messages,
        actions = {
            if (esDueno && ui.mascota != null) EditIcon("Editar") { onEdit(id) }
        },
    ) { padding ->
        when {
            ui.cargando && ui.mascota == null -> LoadingBox(Modifier.padding(padding))
            ui.mascota == null -> ErrorBox(ui.error ?: "No se pudo cargar la mascota.", onRetry, Modifier.padding(padding))
            else -> {
                val m = ui.mascota!!
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (m.fotoUrl != null) {
                            AsyncImage(
                                model = m.fotoUrl,
                                contentDescription = m.nombre,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (esDueno) {
                            FotoSelector(enabled = !ui.ocupado, onFoto = onFotoSelected)
                        }
                        Text(m.nombre, style = MaterialTheme.typography.headlineSmall)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOfNotNull(
                                m.especie?.nombre,
                                m.raza,
                                m.fechaNacimiento?.let { edadTexto(it) },
                                m.pesoKg?.let { "$it kg" },
                            ).forEach { dato -> DatoChip(dato) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (esDueno) {
                                AccesoRapido(Icons.Default.Add, "Agendar", onAgendar, Modifier.weight(1f))
                            }
                            AccesoRapido(Icons.Default.Favorite, "Carné", { onCarne(id) }, Modifier.weight(1f))
                            AccesoRapido(Icons.Default.DateRange, "Citas", { onHistorialCitas(id) }, Modifier.weight(1f))
                        }
                        if (ui.proximas.isNotEmpty()) {
                            Seccion("Próximas dosis")
                            ui.proximas.forEach { dosis -> DosisFila(dosis) }
                        }
                        if (esDueno) {
                            TextButton(
                                onClick = { confirmar = true },
                                enabled = !ui.ocupado,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Eliminar mascota", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmar) {
        ConfirmDialog(
            title = "Eliminar mascota",
            text = "Se eliminará ${ui.mascota?.nombre.orEmpty()} de tu lista. ¿Continuar?",
            confirmText = "Eliminar",
            onConfirm = {
                confirmar = false
                onEliminar()
            },
            onDismiss = { confirmar = false },
        )
    }
}

private fun edadTexto(fechaNacimiento: String): String? {
    val dias = diasHasta(fechaNacimiento)?.let { -it } ?: return null
    if (dias < 0) return null
    val anios = dias / 365
    val meses = (dias % 365) / 30
    return when {
        anios >= 1 -> if (anios == 1) "1 año" else "$anios años"
        meses >= 1 -> if (meses == 1) "1 mes" else "$meses meses"
        else -> "Cachorro"
    }
}

@Composable
private fun DatoChip(texto: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DosisFila(dosis: ProximaDosis) {
    val estado = estadoDosis(dosis.fecha)
    val (fondo, texto) = when (estado.nivel) {
        NivelDosis.VENCIDA -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        NivelDosis.PROXIMA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
    }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(dosis.vacuna.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Próxima: ${fechaCorta(dosis.fecha)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = RoundedCornerShape(50), color = fondo) {
                Text(
                    estado.texto,
                    color = texto,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MascotaDetailScreenPreview() {
    PetCareTheme {
        MascotaDetailContent(
            id = 1,
            ui = MascotaDetailUi(
                mascota = Mascota(
                    id = 1,
                    nombre = "Max",
                    especie = Ref(1, "Perro"),
                    raza = "Golden Retriever",
                    fechaNacimiento = "2020-05-15",
                    pesoKg = 28.5,
                ),
                cargando = false,
            ),
        )
    }
}
