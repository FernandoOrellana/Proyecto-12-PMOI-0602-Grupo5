package com.uth.proyectofinal.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.AuthRepository
import com.uth.proyectofinal.ui.common.ActionButton
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.AppTextField
import com.uth.proyectofinal.ui.common.ErrorText
import com.uth.proyectofinal.ui.common.MessageViewModel
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VerifyUi(
    val codigo: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val verificado: Boolean = false,
)

class VerifyEmailViewModel(
    private val email: String,
    private val auth: AuthRepository,
) : MessageViewModel() {

    private val _ui = MutableStateFlow(VerifyUi())
    val ui: StateFlow<VerifyUi> = _ui.asStateFlow()

    fun onCodigo(value: String) = _ui.update { it.copy(codigo = value.filter { c -> c.isDigit() }.take(6), error = null) }

    fun verificar() {
        val codigo = _ui.value.codigo
        if (!Regex("\\d{6}").matches(codigo)) {
            _ui.update { it.copy(error = "El código debe tener 6 dígitos.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val result = auth.verifyEmail(email, codigo)) {
                is ApiResult.Success -> _ui.update { it.copy(loading = false, verificado = true) }
                is ApiResult.Error -> _ui.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun reenviar() {
        viewModelScope.launch {
            when (val result = auth.resendCode(email)) {
                is ApiResult.Success -> say("Código reenviado a tu correo.")
                is ApiResult.Error -> _ui.update { it.copy(error = result.message) }
            }
        }
    }
}

@Composable
fun VerifyEmailScreen(email: String, onBack: () -> Unit, onVerified: () -> Unit) {
    val vm: VerifyEmailViewModel = viewModel(factory = appFactory { VerifyEmailViewModel(email, it.auth) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.verificado) {
        if (ui.verificado) onVerified()
    }

    VerifyEmailContent(
        email = email,
        ui = ui,
        messages = vm.messages,
        onBack = onBack,
        onCodigoChange = vm::onCodigo,
        onVerificar = vm::verificar,
        onReenviar = vm::reenviar,
    )
}

@Composable
fun VerifyEmailContent(
    email: String,
    ui: VerifyUi,
    messages: Flow<String>? = null,
    onBack: () -> Unit = {},
    onCodigoChange: (String) -> Unit = {},
    onVerificar: () -> Unit = {},
    onReenviar: () -> Unit = {},
) {
    AppScaffold(title = "Verificar correo", onBack = onBack, messages = messages) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Enviamos un código de 6 dígitos a $email. Ingrésalo para activar tu cuenta.")
            AppTextField(
                value = ui.codigo,
                onValueChange = onCodigoChange,
                label = "Código de verificación",
                keyboard = KeyboardType.Number,
            )
            ErrorText(ui.error)
            ActionButton("Verificar", ui.loading, onVerificar)
            OutlinedButton(onClick = onReenviar, modifier = Modifier.fillMaxWidth()) {
                Text("Reenviar código")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifyEmailScreenPreview() {
    PetCareTheme {
        VerifyEmailContent(
            email = "usuario@ejemplo.com",
            ui = VerifyUi(codigo = "123456"),
        )
    }
}
