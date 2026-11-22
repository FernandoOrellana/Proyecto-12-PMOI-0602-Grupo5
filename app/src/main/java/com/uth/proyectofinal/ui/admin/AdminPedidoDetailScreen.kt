package com.uth.proyectofinal.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Pedido
import com.uth.proyectofinal.data.remote.dto.ComprobanteInterno
import com.uth.proyectofinal.data.remote.dto.RegistrarCobroRequest
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.TiendaRepository
import com.uth.proyectofinal.ui.common.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AdminPedidoDetailUi(
    val pedido: Pedido? = null,
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val error: String? = null,
    val comprobante: ComprobanteInterno? = null,
    val consultandoCobro: Boolean = false,
    val cobroConsultado: Boolean = false,
    val enviandoCobro: Boolean = false,
    val errorCobro: String? = null,
)

class AdminPedidoDetailViewModel(private val id: Int, private val tienda: TiendaRepository) : ViewModel() {
    private val _ui = MutableStateFlow(AdminPedidoDetailUi())
    val ui = _ui.asStateFlow()

    fun cargar() {
        if (_ui.value.enviando) return
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            when (val result = tienda.pedido(id)) {
                is ApiResult.Success -> _ui.update { it.copy(pedido = result.data, cargando = false) }
                is ApiResult.Error -> _ui.update { it.copy(cargando = false, error = result.message) }
            }
        }
    }

    fun cambiarEstado(estado: String) {
        val actual = _ui.value
        val permitido = when (actual.pedido?.estado) {
            "PENDIENTE" -> estado == "LISTO_PARA_RECOGER" || estado == "CANCELADO"
            "LISTO_PARA_RECOGER" -> estado == "ENTREGADO" || estado == "CANCELADO"
            else -> false
        }
        if (actual.enviando || actual.enviandoCobro || actual.cargando || actual.consultandoCobro || !permitido) return
        if (estado == "ENTREGADO" && actual.comprobante == null) {
            _ui.update { it.copy(error = "No se puede marcar el pedido como entregado sin haber realizado el cobro.") }
            return
        }
        _ui.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            when (val result = tienda.cambiarEstadoPedido(id, estado)) {
                is ApiResult.Success -> _ui.update { it.copy(pedido = result.data, enviando = false) }
                is ApiResult.Error -> _ui.update { it.copy(enviando = false, error = result.message) }
            }
        }
    }

    fun consultarCobro() {
        if (_ui.value.consultandoCobro || _ui.value.enviandoCobro) return
        _ui.update { it.copy(consultandoCobro = true, cobroConsultado = false, errorCobro = null) }
        viewModelScope.launch {
            when (val result = tienda.cobroPedido(id)) {
                is ApiResult.Success -> _ui.update { it.copy(comprobante = result.data, consultandoCobro = false, cobroConsultado = true) }
                is ApiResult.Error -> _ui.update { it.copy(consultandoCobro = false, errorCobro = result.message) }
            }
        }
    }

    fun cobrar(request: RegistrarCobroRequest) {
        val actual = _ui.value
        if (actual.enviandoCobro || actual.enviando || actual.consultandoCobro || !actual.cobroConsultado ||
            actual.comprobante != null || actual.pedido?.estado != "LISTO_PARA_RECOGER") return
        _ui.update { it.copy(enviandoCobro = true, errorCobro = null) }
        viewModelScope.launch {
            when (val result = tienda.cobrarPedido(id, request)) {
                is ApiResult.Success -> _ui.update { it.copy(comprobante = result.data, enviandoCobro = false, cobroConsultado = true) }
                is ApiResult.Error -> _ui.update { it.copy(enviandoCobro = false, errorCobro = result.message) }
            }
        }
    }
}

@Composable
fun AdminPedidoDetailScreen(id: Int, onBack: () -> Unit) {
    val vm: AdminPedidoDetailViewModel = viewModel(factory = appFactory { AdminPedidoDetailViewModel(id, it.tienda) })
    val ui by vm.ui.collectAsStateWithLifecycle()
    var confirmarCancelacion by remember { mutableStateOf(false) }
    var mostrarCobro by rememberSaveable(id) { mutableStateOf(false) }
    var metodo by rememberSaveable(id) { mutableStateOf("EFECTIVO") }
    var referencia by rememberSaveable(id) { mutableStateOf("") }
    var clave by rememberSaveable(id) { mutableStateOf(UUID.randomUUID().toString()) }
    var intentoEnviado by rememberSaveable(id) { mutableStateOf(false) }
    OnResume { vm.cargar(); vm.consultarCobro() }

    AppScaffold(title = "Pedido #$id", onBack = onBack) { padding ->
        val pedido = ui.pedido
        when {
            ui.cargando && pedido == null -> LoadingBox(Modifier.padding(padding))
            pedido == null -> ErrorBox(ui.error ?: "No se pudo cargar el pedido.", vm::cargar, Modifier.padding(padding))
            else -> Column(
                Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EstadoBadge(pedido.estado.replace('_', ' '))
                Text("Cliente: ${pedido.cliente?.nombre ?: "No disponible"}")
                Text("Fecha: ${pedido.fecha.textoFecha()}")
                Text("Total: ${pedido.total.lps()}", style = MaterialTheme.typography.titleMedium)
                HorizontalDivider()
                Text("Productos", style = MaterialTheme.typography.titleMedium)
                if (pedido.items.isEmpty()) Text("El pedido no tiene productos.")
                pedido.items.forEach { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(item.nombre, style = MaterialTheme.typography.titleSmall)
                            Text("${item.cantidad} x ${item.precioUnitario.lps()} = ${item.subtotal.lps()}")
                        }
                    }
                }
                HorizontalDivider()
                Text("Cobro", style = MaterialTheme.typography.titleMedium)
                if (ui.consultandoCobro) CircularProgressIndicator()
                ui.comprobante?.let { comprobante -> ComprobantePedido(comprobante) }
                ui.errorCobro?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!ui.consultandoCobro && !ui.enviandoCobro && ui.comprobante == null && (!ui.cobroConsultado || ui.errorCobro != null)) {
                    TextButton(onClick = vm::consultarCobro) { Text("Consultar cobro") }
                }
                if (pedido.estado == "LISTO_PARA_RECOGER" && ui.cobroConsultado && ui.comprobante == null) {
                    Button(onClick = { mostrarCobro = true }, enabled = !ui.enviandoCobro && !ui.enviando && !ui.consultandoCobro, modifier = Modifier.fillMaxWidth()) {
                        Text(if (intentoEnviado) "Reintentar cobro" else "Cobrar")
                    }
                }
                if (ui.enviandoCobro) CircularProgressIndicator()
                ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (ui.enviando) CircularProgressIndicator()
                if (pedido.estado == "PENDIENTE") {
                    Button(onClick = { vm.cambiarEstado("LISTO_PARA_RECOGER") }, enabled = !ui.enviando && !ui.enviandoCobro, modifier = Modifier.fillMaxWidth()) { Text("Marcar listo para recoger") }
                }
                if (pedido.estado == "LISTO_PARA_RECOGER") {
                    val cobrado = ui.comprobante != null
                    Button(
                        onClick = { vm.cambiarEstado("ENTREGADO") },
                        enabled = !ui.enviando && !ui.enviandoCobro && !ui.consultandoCobro && cobrado,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Marcar entregado") }
                    if (!cobrado) {
                        Text(
                            "Debes registrar el cobro antes de marcar el pedido como entregado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (pedido.estado == "PENDIENTE" || pedido.estado == "LISTO_PARA_RECOGER") {
                    OutlinedButton(onClick = { confirmarCancelacion = true }, enabled = !ui.enviando && !ui.enviandoCobro, modifier = Modifier.fillMaxWidth()) { Text("Cancelar pedido") }
                }
            }
        }
    }
    if (mostrarCobro && ui.comprobante == null) {
        val referenciaValida = referencia.trim().length <= 120 && referencia.none { Character.isISOControl(it) }
        AlertDialog(
            onDismissRequest = { if (!ui.enviandoCobro) mostrarCobro = false },
            title = { Text("Cobrar pedido #$id") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total: ${ui.pedido?.total?.lps() ?: "—"}")
                    listOf("EFECTIVO" to "Efectivo", "TRANSFERENCIA" to "Transferencia bancaria").forEach { (valor, etiqueta) ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = metodo == valor, onClick = { metodo = valor }, enabled = !intentoEnviado && !ui.enviandoCobro)
                            Text(etiqueta)
                        }
                    }
                    if (metodo == "TRANSFERENCIA") {
                        OutlinedTextField(value = referencia, onValueChange = { referencia = it }, label = { Text("Referencia bancaria (opcional)") },
                            enabled = !intentoEnviado && !ui.enviandoCobro, isError = !referenciaValida, singleLine = true)
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
                }, enabled = !ui.enviandoCobro && referenciaValida) { Text(if (intentoEnviado) "Reintentar" else "Registrar cobro") }
            },
            dismissButton = { TextButton(onClick = { mostrarCobro = false }, enabled = !ui.enviandoCobro) { Text("Volver") } },
        )
    }
    if (confirmarCancelacion) {
        ConfirmDialog(
            title = "Cancelar pedido", text = "¿Cancelar este pedido? Los productos volverán al inventario.",
            confirmText = "Cancelar pedido", onConfirm = { confirmarCancelacion = false; vm.cambiarEstado("CANCELADO") },
            onDismiss = { confirmarCancelacion = false },
        )
    }
}

@Composable
private fun ComprobantePedido(comprobante: ComprobanteInterno) {
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
