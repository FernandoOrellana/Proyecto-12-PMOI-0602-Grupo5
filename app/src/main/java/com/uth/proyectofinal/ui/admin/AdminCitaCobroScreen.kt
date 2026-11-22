package com.uth.proyectofinal.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Cita
import com.uth.proyectofinal.data.remote.dto.CitaEstado
import com.uth.proyectofinal.data.remote.dto.ComprobanteInterno
import com.uth.proyectofinal.data.remote.dto.RegistrarCobroRequest
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.ui.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AdminCitaCobroUi(
    val cita: Cita? = null,
    val cargando: Boolean = true,
    val error: String? = null,
    val comprobante: ComprobanteInterno? = null,
    val consultando: Boolean = false,
    val consultado: Boolean = false,
    val enviando: Boolean = false,
    val errorCobro: String? = null,
)

class AdminCitaCobroViewModel(private val id: Int, private val repo: CitaRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AdminCitaCobroUi())
    val ui = _ui.asStateFlow()

    fun cargar() {
        if (_ui.value.enviando) return
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            when (val result = repo.get(id)) {
                is ApiResult.Success -> _ui.update { it.copy(cita = result.data, cargando = false) }
                is ApiResult.Error -> _ui.update { it.copy(cargando = false, error = result.message) }
            }
        }
    }

    fun consultarCobro() {
        if (_ui.value.consultando || _ui.value.enviando) return
        _ui.update { it.copy(consultando = true, consultado = false, errorCobro = null) }
        viewModelScope.launch {
            when (val result = repo.cobroCita(id)) {
                is ApiResult.Success -> _ui.update { it.copy(comprobante = result.data, consultando = false, consultado = true) }
                is ApiResult.Error -> _ui.update { it.copy(consultando = false, errorCobro = result.message) }
            }
        }
    }

    fun cobrar(request: RegistrarCobroRequest) {
        val actual = _ui.value
        if (actual.enviando || actual.consultando || !actual.consultado || actual.comprobante != null ||
            actual.cita?.estado != CitaEstado.ATENDIDA) return
        _ui.update { it.copy(enviando = true, errorCobro = null) }
        viewModelScope.launch {
            when (val result = repo.cobrarCita(id, request)) {
                is ApiResult.Success -> _ui.update { it.copy(comprobante = result.data, enviando = false, consultado = true) }
                is ApiResult.Error -> _ui.update { it.copy(enviando = false, errorCobro = result.message) }
            }
        }
    }
}

@Composable
fun AdminCitaCobroScreen(id: Int, onBack: () -> Unit) {
    val vm: AdminCitaCobroViewModel = viewModel(factory = appFactory { AdminCitaCobroViewModel(id, it.citas) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    var mostrarCobro by rememberSaveable(id) { mutableStateOf(false) }
    var metodo by rememberSaveable(id) { mutableStateOf("EFECTIVO") }
    var referencia by rememberSaveable(id) { mutableStateOf("") }
    val clave = rememberSaveable(id) { UUID.randomUUID().toString() }
    var intentoEnviado by rememberSaveable(id) { mutableStateOf(false) }
    OnResume { vm.cargar(); vm.consultarCobro() }

    AppScaffold(title = "Cita #$id", onBack = onBack) { padding ->
        val cita = ui.cita
        when {
            ui.cargando && cita == null -> LoadingBox(Modifier.padding(padding))
            cita == null -> ErrorBox(ui.error ?: "No se pudo cargar la cita.", vm::cargar, Modifier.padding(padding))
            else -> Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                EstadoBadge(cita.estado)
                Text("Cliente: ${cita.dueno?.nombre ?: "No disponible"}")
                Text("Mascota: ${cita.mascota.nombre}")
                Text("Servicio: ${cita.servicio.nombre}")
                Text("Fecha: ${cita.fecha} ${cita.hora}")
                Text("Precio actual del servicio: ${cita.servicio.precio.lps()}", style = MaterialTheme.typography.titleSmall)
                Text("Precio de referencia. El importe definitivo puede variar y aparecerá en el comprobante.", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Cobro", style = MaterialTheme.typography.titleMedium)
                if (ui.consultando || ui.enviando) CircularProgressIndicator()
                ui.comprobante?.let { ComprobanteCita(it) }
                ui.errorCobro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!ui.consultando && !ui.enviando && ui.comprobante == null && (!ui.consultado || ui.errorCobro != null)) {
                    TextButton(onClick = vm::consultarCobro) { Text("Consultar cobro") }
                }
                if (cita.estado == CitaEstado.ATENDIDA && ui.consultado && ui.comprobante == null) {
                    Button(onClick = { mostrarCobro = true }, enabled = !ui.enviando && !ui.consultando, modifier = Modifier.fillMaxWidth()) {
                        Text(if (intentoEnviado) "Reintentar cobro" else "Cobrar")
                    }
                }
            }
        }
    }
    if (mostrarCobro && ui.comprobante == null) {
        val referenciaValida = referencia.trim().length <= 120 && referencia.none { Character.isISOControl(it) }
        AlertDialog(
            onDismissRequest = { if (!ui.enviando) mostrarCobro = false },
            title = { Text("Cobrar cita #$id") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Precio actual de referencia: ${ui.cita?.servicio?.precio?.lps() ?: "—"}")
                    Text("El importe definitivo lo calcula el servidor y puede variar.", style = MaterialTheme.typography.bodySmall)
                    listOf("EFECTIVO" to "Efectivo", "TRANSFERENCIA" to "Transferencia bancaria").forEach { (valor, etiqueta) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = metodo == valor, onClick = { metodo = valor }, enabled = !intentoEnviado && !ui.enviando)
                            Text(etiqueta)
                        }
                    }
                    if (metodo == "TRANSFERENCIA") {
                        OutlinedTextField(value = referencia, onValueChange = { referencia = it }, label = { Text("Referencia bancaria (opcional)") },
                            enabled = !intentoEnviado && !ui.enviando, isError = !referenciaValida, singleLine = true)
                        if (!referenciaValida) Text("Máximo 120 caracteres, sin caracteres de control.", color = MaterialTheme.colorScheme.error)
                        Text("La referencia no verifica la transferencia automáticamente.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (intentoEnviado) Text("Se conservarán los datos y la clave para reintentar esta operación.", style = MaterialTheme.typography.bodySmall)
                    ui.errorCobro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    intentoEnviado = true
                    vm.cobrar(RegistrarCobroRequest(metodo, if (metodo == "TRANSFERENCIA") referencia.trim().ifEmpty { null } else null, clave))
                    mostrarCobro = false
                }, enabled = !ui.enviando && referenciaValida) { Text(if (intentoEnviado) "Reintentar" else "Registrar cobro") }
            },
            dismissButton = { TextButton(onClick = { mostrarCobro = false }, enabled = !ui.enviando) { Text("Volver") } },
        )
    }
}

@Composable
private fun ComprobanteCita(comprobante: ComprobanteInterno) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Comprobante interno ${comprobante.numeroInterno}", style = MaterialTheme.typography.titleMedium)
            Text("Fecha: ${comprobante.fecha.textoFecha()}")
            Text("Cliente: ${comprobante.cliente.nombre}")
            Text("Registrado por: ${comprobante.administrador.nombre}")
            Text("Método: ${if (comprobante.metodo == "TRANSFERENCIA") "Transferencia bancaria" else "Efectivo"}")
            comprobante.referenciaTransferencia?.let { Text("Referencia: $it") }
            comprobante.items.forEach { Text("${it.descripcion}: ${it.cantidad} x ${it.precioUnitario.lps()} = ${it.subtotal.lps()}") }
            Text("Importe: ${comprobante.importe.lps()}", style = MaterialTheme.typography.titleSmall)
        }
    }
}
