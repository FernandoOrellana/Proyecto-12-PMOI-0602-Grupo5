package com.uth.proyectofinal.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
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
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.AuthRepository
import com.uth.proyectofinal.ui.common.ActionButton
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.AppTextField
import com.uth.proyectofinal.ui.common.ErrorText
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ForgotUi(
    val email: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val enviado: String? = null,
)

class ForgotPasswordViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _ui = MutableStateFlow(ForgotUi())
    val ui: StateFlow<ForgotUi> = _ui.asStateFlow()

    fun onEmail(value: String) = _ui.update { it.copy(email = value, error = null) }

    fun enviar() {
        val email = _ui.value.email.trim()
        if (!email.contains("@")) {
            _ui.update { it.copy(error = "Ingresa un correo válido.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val result = auth.forgotPassword(email)) {
                is ApiResult.Success -> _ui.update { it.copy(loading = false, enviado = email) }
                is ApiResult.Error -> _ui.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, onSent: (String) -> Unit) {
    val vm: ForgotPasswordViewModel = viewModel(factory = appFactory { ForgotPasswordViewModel(it.auth) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.enviado) {
        ui.enviado?.let(onSent)
    }

    ForgotPasswordContent(
        ui = ui,
        onBack = onBack,
        onEmailChange = vm::onEmail,
        onEnviar = vm::enviar,
    )
}

@Composable
fun ForgotPasswordContent(
    ui: ForgotUi,
    onBack: () -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onEnviar: () -> Unit = {},
) {
    AppScaffold(title = "Recuperar contraseña", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Te enviaremos una clave temporal a tu correo registrado.")
            AppTextField(
                value = ui.email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                keyboard = KeyboardType.Email,
            )
            ErrorText(ui.error)
            ActionButton("Enviar clave temporal", ui.loading, onEnviar)
        }
    }
}

data class ResetUi(
    val email: String,
    val claveTemporal: String = "",
    val nuevaPassword: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val listo: Boolean = false,
)

class ResetPasswordViewModel(email: String, private val auth: AuthRepository) : ViewModel() {

    private val _ui = MutableStateFlow(ResetUi(email))
    val ui: StateFlow<ResetUi> = _ui.asStateFlow()

    fun onEmail(value: String) = _ui.update { it.copy(email = value, error = null) }
    fun onClave(value: String) = _ui.update { it.copy(claveTemporal = value, error = null) }
    fun onNueva(value: String) = _ui.update { it.copy(nuevaPassword = value, error = null) }

    fun cambiar() {
        val s = _ui.value
        val validation = when {
            s.email.isBlank() -> "Ingresa tu correo."
            s.claveTemporal.isBlank() -> "Ingresa la clave temporal."
            s.nuevaPassword.length < 8 -> "La nueva contraseña debe tener al menos 8 caracteres."
            else -> null
        }
        if (validation != null) {
            _ui.update { it.copy(error = validation) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val result = auth.resetPassword(s.email.trim(), s.claveTemporal.trim(), s.nuevaPassword)) {
                is ApiResult.Success -> _ui.update { it.copy(loading = false, listo = true) }
                is ApiResult.Error -> _ui.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(email: String, onBack: () -> Unit, onDone: () -> Unit) {
    val vm: ResetPasswordViewModel = viewModel(factory = appFactory { ResetPasswordViewModel(email, it.auth) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.listo) {
        if (ui.listo) onDone()
    }

    ResetPasswordContent(
        ui = ui,
        onBack = onBack,
        onEmailChange = vm::onEmail,
        onClaveChange = vm::onClave,
        onNuevaChange = vm::onNueva,
        onCambiar = vm::cambiar,
    )
}

@Composable
fun ResetPasswordContent(
    ui: ResetUi,
    onBack: () -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onClaveChange: (String) -> Unit = {},
    onNuevaChange: (String) -> Unit = {},
    onCambiar: () -> Unit = {},
) {
    AppScaffold(title = "Nueva contraseña", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField(ui.email, onEmailChange, "Correo electrónico", keyboard = KeyboardType.Email)
            AppTextField(ui.claveTemporal, onClaveChange, "Clave temporal recibida")
            AppTextField(ui.nuevaPassword, onNuevaChange, "Nueva contraseña (mínimo 8)", password = true)
            ErrorText(ui.error)
            ActionButton("Cambiar contraseña", ui.loading, onCambiar)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    PetCareTheme {
        ForgotPasswordContent(
            ui = ForgotUi(email = "usuario@ejemplo.com"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResetPasswordScreenPreview() {
    PetCareTheme {
        ResetPasswordContent(
            ui = ResetUi(
                email = "usuario@ejemplo.com",
                claveTemporal = "123456",
            ),
        )
    }
}
