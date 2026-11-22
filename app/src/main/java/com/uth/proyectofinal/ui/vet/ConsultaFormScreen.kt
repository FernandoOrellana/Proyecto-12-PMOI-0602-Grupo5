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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.ConsultaRequest
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.ui.common.ActionButton
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.AppTextField
import com.uth.proyectofinal.ui.common.ErrorText
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.common.opcional
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConsultaUi(
    val diagnostico: String = "",
    val tratamiento: String = "",
    val observaciones: String = "",
    val peso: String = "",
    val guardando: Boolean = false,
    val error: String? = null,
    val listo: Boolean = false,
)

class ConsultaFormViewModel(private val citaId: Int, private val citas: CitaRepository) : ViewModel() {

    private val _ui = MutableStateFlow(ConsultaUi())
    val ui: StateFlow<ConsultaUi> = _ui.asStateFlow()

    fun onDiagnostico(value: String) = _ui.update { it.copy(diagnostico = value, error = null) }
    fun onTratamiento(value: String) = _ui.update { it.copy(tratamiento = value, error = null) }
    fun onObservaciones(value: String) = _ui.update { it.copy(observaciones = value, error = null) }
    fun onPeso(value: String) = _ui.update { it.copy(peso = value, error = null) }

    fun guardar() {
        val s = _ui.value
        val peso = s.peso.opcional()?.toDoubleOrNull()
        val validation = when {
            s.diagnostico.isBlank() -> "Ingresa el diagnóstico."
            s.peso.isNotBlank() && peso == null -> "El peso debe ser un número."
            else -> null
        }
        if (validation != null) {
            _ui.update { it.copy(error = validation) }
            return
        }
        val body = ConsultaRequest(
            diagnostico = s.diagnostico.trim(),
            tratamiento = s.tratamiento.opcional(),
            observaciones = s.observaciones.opcional(),
            peso = peso,
        )
        viewModelScope.launch {
            _ui.update { it.copy(guardando = true, error = null) }
            when (val result = citas.registrarConsulta(citaId, body)) {
                is ApiResult.Success -> _ui.update { it.copy(guardando = false, listo = true) }
                is ApiResult.Error -> _ui.update { it.copy(guardando = false, error = result.message) }
            }
        }
    }
}

@Composable
fun ConsultaFormScreen(citaId: Int, onBack: () -> Unit) {
    val vm: ConsultaFormViewModel = viewModel(factory = appFactory { ConsultaFormViewModel(citaId, it.citas) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.listo) {
        if (ui.listo) onBack()
    }

    ConsultaFormContent(
        ui = ui,
        onBack = onBack,
        onDiagnostico = vm::onDiagnostico,
        onTratamiento = vm::onTratamiento,
        onObservaciones = vm::onObservaciones,
        onPeso = vm::onPeso,
        onGuardar = vm::guardar,
    )
}

@Composable
fun ConsultaFormContent(
    ui: ConsultaUi,
    onBack: () -> Unit = {},
    onDiagnostico: (String) -> Unit = {},
    onTratamiento: (String) -> Unit = {},
    onObservaciones: (String) -> Unit = {},
    onPeso: (String) -> Unit = {},
    onGuardar: () -> Unit = {},
) {
    AppScaffold(title = "Registrar consulta", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField(ui.diagnostico, onDiagnostico, "Diagnóstico *")
            AppTextField(ui.tratamiento, onTratamiento, "Tratamiento")
            AppTextField(ui.observaciones, onObservaciones, "Observaciones", singleLine = false)
            AppTextField(ui.peso, onPeso, "Peso en kg", keyboard = KeyboardType.Decimal)
            ErrorText(ui.error)
            ActionButton("Guardar consulta", ui.guardando, onGuardar)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsultaFormScreenPreview() {
    PetCareTheme {
        ConsultaFormContent(
            ui = ConsultaUi(
                diagnostico = "Gripe canina leve",
                tratamiento = "Jarabe antitusivo cada 8 horas",
                observaciones = "Revisar en 5 días",
                peso = "12.5",
            ),
        )
    }
}
