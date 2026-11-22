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

data class MascotaDetailUi(
    val mascota: Mascota? = null,
    val cargando: Boolean = true,
    val error: String? = null,
    val ocupado: Boolean = false,
    val eliminada: Boolean = false,
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
    )
}

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
                        .padding(16.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
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
                            Text("Sin foto", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (esDueno) {
                        FotoSelector(
                            enabled = !ui.ocupado,
                            onFoto = onFotoSelected,
                        )
                    }

                    Text("Especie: ${m.especie?.nombre.orEmpty()}", style = MaterialTheme.typography.bodyLarge)
                    Text("Raza: ${m.raza ?: "No registrada"}", style = MaterialTheme.typography.bodyLarge)
                    Text("Nacimiento: ${m.fechaNacimiento ?: "No registrada"}", style = MaterialTheme.typography.bodyLarge)
                    Text("Peso: ${m.pesoKg?.let { "$it kg" } ?: "No registrado"}", style = MaterialTheme.typography.bodyLarge)

                    Button(onClick = { onHistorialCitas(id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Historial de citas")
                    }
                    OutlinedButton(onClick = { onCarne(id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Carné e historial médico")
                    }
                    if (esDueno) {
                        Button(
                            onClick = { confirmar = true },
                            enabled = !ui.ocupado,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Eliminar mascota") }
                    }
                    TextButton(onClick = onRetry) { Text("Actualizar") }
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
