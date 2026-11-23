@file:OptIn(ExperimentalMaterial3Api::class)

package com.uth.proyectofinal.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.local.ClinicaStore
import com.uth.proyectofinal.data.local.DatosClinica
import com.uth.proyectofinal.ui.common.ActionButton
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.AppTextField
import com.uth.proyectofinal.ui.common.CampoTelefono
import com.uth.proyectofinal.ui.common.PAISES
import com.uth.proyectofinal.ui.common.Pais
import com.uth.proyectofinal.ui.common.componer
import com.uth.proyectofinal.ui.common.separarTelefono
import com.uth.proyectofinal.ui.common.ErrorText
import com.uth.proyectofinal.ui.common.MessageViewModel
import com.uth.proyectofinal.ui.common.SectionTitle
import com.uth.proyectofinal.ui.common.VeterinariaCard
import com.uth.proyectofinal.ui.common.appFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ClinicaUi(
    val datos: DatosClinica = DatosClinica(),
    val pais: Pais = PAISES.first(),
    val numero: String = "",
    val error: String? = null,
)

class ClinicaViewModel(private val store: ClinicaStore) : MessageViewModel() {

    private val _ui: MutableStateFlow<ClinicaUi>
    val ui: StateFlow<ClinicaUi>

    init {
        val actuales = store.datos.value
        val (pais, numero) = separarTelefono(actuales.telefono)
        _ui = MutableStateFlow(ClinicaUi(actuales, pais, numero))
        ui = _ui.asStateFlow()
    }

    fun editar(cambio: (DatosClinica) -> DatosClinica) = _ui.update { it.copy(datos = cambio(it.datos), error = null) }

    fun onPais(pais: Pais) = _ui.update { it.copy(pais = pais, numero = it.numero.take(pais.digitos), error = null) }

    fun onNumero(valor: String) = _ui.update {
        it.copy(numero = valor.filter { c -> c.isDigit() }.take(it.pais.digitos), error = null)
    }

    fun guardar() {
        val s = _ui.value
        val error = when {
            s.datos.nombre.isBlank() -> "Escribe el nombre de la veterinaria."
            s.numero.isNotEmpty() && s.numero.length != s.pais.digitos ->
                "El número de ${s.pais.nombre} debe tener ${s.pais.digitos} dígitos."
            else -> null
        }
        if (error != null) {
            _ui.update { it.copy(error = error) }
            return
        }
        val telefono = s.pais.componer(s.numero)
        store.guardar(s.datos.copy(telefono = telefono))
        say("Datos guardados.")
    }
}

@Composable
fun ClinicaScreen(onBack: () -> Unit) {
    val vm: ClinicaViewModel = viewModel(factory = appFactory { ClinicaViewModel(it.clinica) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    AppScaffold(title = "Datos de la veterinaria", onBack = onBack, messages = vm.messages) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Estos datos se muestran en la tarjeta de la veterinaria, en el botón Cómo llegar y en los recordatorios del calendario.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppTextField(ui.datos.nombre, { v -> vm.editar { it.copy(nombre = v) } }, "Nombre")
            AppTextField(ui.datos.direccion, { v -> vm.editar { it.copy(direccion = v) } }, "Dirección")
            CampoTelefono(ui.pais, ui.numero, vm::onPais, vm::onNumero)
            AppTextField(ui.datos.horario, { v -> vm.editar { it.copy(horario = v) } }, "Horario de atención")
            ErrorText(ui.error)
            ActionButton("Guardar", false, vm::guardar)
            SectionTitle("Así lo verán los usuarios")
            VeterinariaCard()
        }
    }
}
