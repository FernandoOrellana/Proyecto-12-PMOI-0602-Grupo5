package com.uth.proyectofinal.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.uth.proyectofinal.push.PushRegistrar
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

data class LoginUi(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val sinVerificar: Boolean = false,
    val rol: String? = null,
)

class LoginViewModel(
    private val auth: AuthRepository,
    private val push: PushRegistrar,
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUi())
    val ui: StateFlow<LoginUi> = _ui.asStateFlow()

    fun onEmail(value: String) = _ui.update { it.copy(email = value, error = null, sinVerificar = false) }

    fun onPassword(value: String) = _ui.update { it.copy(password = value, error = null) }

    fun login() {
        val state = _ui.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _ui.update { it.copy(error = "Ingresa tu correo y contraseña.") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            when (val result = auth.login(state.email.trim(), state.password)) {
                is ApiResult.Success -> {
                    push.sync()
                    _ui.update { it.copy(loading = false, rol = result.data.rol) }
                }
                is ApiResult.Error -> _ui.update {
                    it.copy(
                        loading = false,
                        error = result.message,
                        sinVerificar = result.code == "EMAIL_NOT_VERIFIED",
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    onVerify: (String) -> Unit,
    onLoggedIn: (String) -> Unit,
) {
    val vm: LoginViewModel = viewModel(factory = appFactory { LoginViewModel(it.auth, it.push) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    LaunchedEffect(ui.rol) {
        ui.rol?.let(onLoggedIn)
    }

    LoginContent(
        ui = ui,
        onEmailChange = vm::onEmail,
        onPasswordChange = vm::onPassword,
        onLogin = vm::login,
        onRegister = onRegister,
        onForgot = onForgot,
        onVerify = onVerify,
    )
}

@Composable
fun LoginContent(
    ui: LoginUi,
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onLogin: () -> Unit = {},
    onRegister: () -> Unit = {},
    onForgot: () -> Unit = {},
    onVerify: (String) -> Unit = {},
) {
    AppScaffold(title = "Iniciar sesión") { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("PetCare HN", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text("Veterinaria Huellitas", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = ui.email,
                onValueChange = onEmailChange,
                label = "Correo electrónico",
                keyboard = KeyboardType.Email,
            )
            AppTextField(
                value = ui.password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                password = true,
            )
            ErrorText(ui.error)
            if (ui.sinVerificar) {
                TextButton(onClick = { onVerify(ui.email.trim()) }) { Text("Verificar mi correo") }
            }
            ActionButton("Iniciar sesión", ui.loading, onLogin)
            TextButton(onClick = onRegister) { Text("Crear cuenta") }
            TextButton(onClick = onForgot) { Text("¿Olvidaste tu contraseña?") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    PetCareTheme {
        LoginContent(
            ui = LoginUi(
                email = "usuario@ejemplo.com",
            ),
        )
    }
}
