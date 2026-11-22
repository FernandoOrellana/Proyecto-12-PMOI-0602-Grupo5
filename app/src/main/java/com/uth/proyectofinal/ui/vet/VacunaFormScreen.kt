package com.uth.proyectofinal.ui.vet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.VacunaRequest
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CatalogRepository
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.ui.common.ActionButton
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.AppTextField
import com.uth.proyectofinal.ui.common.ErrorText
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.Option
import com.uth.proyectofinal.ui.common.SelectField
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.esFecha
import com.uth.proyectofinal.ui.common.isoFecha
import com.uth.proyectofinal.ui.common.opcional
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VacunaUi(
    val vacunas: List<Option> = emptyList(),
    val vacunaId: Int? = null,
    val fechaAplicacion: String = isoFecha(),
    val lote: String = "",
    val proximaDosis: String = "",
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val error: String? = null,
    val listo: Boolean = false,
)

class VacunaFormViewModel(
    private val citaId: Int,
    private val mascotaId: Int,
    private val mascotas: MascotaRepository,
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(VacunaUi())
    val ui: StateFlow<VacunaUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = catalog.vacunas()) {
                is ApiResult.Success -> _ui.update {
                    it.copy(cargando = false, vacunas = result.data.map { v -> Option(v.id, v.nombre) })
                }
                is ApiResult.Error -> _ui.update { it.copy(cargando = false, error = result.message) }
            }
        }
    }

    fun onVacuna(id: Int) = _ui.update { it.copy(vacunaId = id, error = null) }
    fun onFecha(value: String) = _ui.update { it.copy(fechaAplicacion = value, error = null) }
    fun onLote(value: String) = _ui.update { it.copy(lote = value, error = null) }
    fun onProxima(value: String) = _ui.update { it.copy(proximaDosis = value, error = null) }

    fun guardar() {
        val s = _ui.value
        val vacunaId = s.vacunaId
        val proxima = s.proximaDosis.opcional()
        val validation = when {
            vacunaId == null -> "Selecciona la vacuna."
            !s.fechaAplicacion.esFecha() -> "La fecha de aplicación debe tener formato AAAA-MM-DD."
            proxima != null && !proxima.esFecha() -> "La próxima dosis debe tener formato AAAA-MM-DD."
            else -> null
        }
        if (validation != null || vacunaId == null) {
            _ui.update { it.copy(error = validation) }
            return
        }
        val body = VacunaRequest(
            vacunaId = vacunaId,
            fechaAplicacion = s.fechaAplicacion,
            lote = s.lote.opcional(),
            proximaDosis = proxima,
            citaId = citaId,
        )
        viewModelScope.launch {
            _ui.update { it.copy(guardando = true, error = null) }
            when (val result = mascotas.registrarVacuna(mascotaId, body)) {
                is ApiResult.Success -> _ui.update { it.copy(guardando = false, listo = true) }
                is ApiResult.Error -> _ui.update { it.copy(guardando = false, error = result.message) }
            }
        }
    }
}

@Composable
fun VacunaFormScreen(citaId: Int, mascotaId: Int, onBack: () -> Unit) {
    val vm: VacunaFormViewModel = viewModel(
        factory = appFactory { VacunaFormViewModel(citaId, mascotaId, it.mascotas, it.catalog) },
    )
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.listo) {
        if (ui.listo) onBack()
    }

    VacunaFormContent(
        ui = ui,
        onBack = onBack,
        onVacuna = vm::onVacuna,
        onFecha = vm::onFecha,
        onLote = vm::onLote,
        onProxima = vm::onProxima,
        onGuardar = vm::guardar,
    )
}

@Composable
fun VacunaFormContent(
    ui: VacunaUi,
    onBack: () -> Unit = {},
    onVacuna: (Int) -> Unit = {},
    onFecha: (String) -> Unit = {},
    onLote: (String) -> Unit = {},
    onProxima: (String) -> Unit = {},
    onGuardar: () -> Unit = {},
) {
    AppScaffold(title = "Registrar vacuna", onBack = onBack) { padding ->
        if (ui.cargando) {
            LoadingBox(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(24.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SelectField("Vacuna", ui.vacunas, ui.vacunaId, onVacuna)
                AppTextField(ui.fechaAplicacion, onFecha, "Fecha de aplicación (AAAA-MM-DD)")
                AppTextField(ui.lote, onLote, "Lote (opcional)")
                AppTextField(ui.proximaDosis, onProxima, "Próxima dosis (AAAA-MM-DD, opcional)")
                ErrorText(ui.error)
                ActionButton("Guardar vacuna", ui.guardando, onGuardar)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VacunaFormScreenPreview() {
    PetCareTheme {
        VacunaFormContent(
            ui = VacunaUi(
                vacunas = listOf(
                    Option(1, "Rabia"),
                    Option(2, "Parvovirus"),
                    Option(3, "Triple Felina"),
                ),
                vacunaId = 1,
                fechaAplicacion = "2026-03-30",
                lote = "LOT-12345",
                proximaDosis = "2027-03-30",
                cargando = false,
            ),
        )
    }
}
