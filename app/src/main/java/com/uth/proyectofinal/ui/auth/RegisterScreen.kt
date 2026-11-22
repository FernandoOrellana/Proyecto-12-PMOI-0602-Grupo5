package com.uth.proyectofinal.ui.auth

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
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.AuthRepository
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

data class RegisterUi(
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val registrado: String? = null,
)

class RegisterViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _ui = MutableStateFlow(RegisterUi())
    val ui: StateFlow<RegisterUi> = _ui.asStateFlow()

    fun onNombre(value: String) = _ui.update { it.copy(nombre = value, error = null) }
    fun onEmail(value: String) = _ui.update { it.copy(email = value, error = null) }
    fun onTelefono(value: String) = _ui.update { it.copy(telefono = value, error = null) }
    fun onPassword(value: String) = _ui.update { it.copy(password = value, error = null) }

    fun registrar() {
        val s = _ui.value
        val validation = when {
            s.nombre.isBlank() -> "Ingresa tu nombre."
            !s.email.contains("@") -> "Ingresa un correo válido."
            s.password.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
            else -> null
        }
        if (validation != null) {
            _ui.update { it.copy(error = validation) }
            return
        }
        val email = s.email.trim()
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val result = auth.register(s.nombre.trim(), email, s.password, s.telefono.opcional())) {
                is ApiResult.Success -> _ui.update { it.copy(loading = false, registrado = email) }
                is ApiResult.Error -> _ui.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}

@Composable
fun RegisterScreen(onBack: () -> Unit, onRegistered: (String) -> Unit) {
    val vm: RegisterViewModel = viewModel(factory = appFactory { RegisterViewModel(it.auth) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.registrado) {
        ui.registrado?.let(onRegistered)
    }

    RegisterContent(
        ui = ui,
        onBack = onBack,
        onNombre = vm::onNombre,
        onEmail = vm::onEmail,
        onTelefono = vm::onTelefono,
        onPassword = vm::onPassword,
        onRegistrar = vm::registrar,
    )
}

@Composable
fun RegisterContent(
    ui: RegisterUi,
    onBack: () -> Unit = {},
    onNombre: (String) -> Unit = {},
    onEmail: (String) -> Unit = {},
    onTelefono: (String) -> Unit = {},
    onPassword: (String) -> Unit = {},
    onRegistrar: () -> Unit = {},
) {
    AppScaffold(title = "Crear cuenta", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppTextField(ui.nombre, onNombre, "Nombre completo")
            AppTextField(ui.email, onEmail, "Correo electrónico", keyboard = KeyboardType.Email)
            AppTextField(ui.telefono, onTelefono, "Teléfono (opcional)", keyboard = KeyboardType.Phone)
            AppTextField(ui.password, onPassword, "Contraseña (mínimo 8 caracteres)", password = true)
            ErrorText(ui.error)
            ActionButton("Registrarme", ui.loading, onRegistrar)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegisterScreenPreview() {
    PetCareTheme {
        RegisterContent(
            ui = RegisterUi(
                nombre = "Juan Pérez",
                email = "juan@ejemplo.com",
                telefono = "9999-8888",
            ),
        )
    }
}
