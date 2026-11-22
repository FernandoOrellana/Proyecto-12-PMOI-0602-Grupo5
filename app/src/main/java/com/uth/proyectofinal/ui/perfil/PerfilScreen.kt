package com.uth.proyectofinal.ui.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.local.SessionUser
import com.uth.proyectofinal.data.repository.AuthRepository
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.ConfirmDialog
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.theme.PetCareTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PerfilViewModel(private val auth: AuthRepository) : ViewModel() {

    val usuario: SessionUser? = auth.user

    private val _ocupado = MutableStateFlow(false)
    val ocupado: StateFlow<Boolean> = _ocupado.asStateFlow()

    private val _salio = MutableStateFlow(false)
    val salio: StateFlow<Boolean> = _salio.asStateFlow()

    fun cerrarSesion() {
        viewModelScope.launch {
            _ocupado.update { true }
            auth.logout()
            _salio.value = true
        }
    }
}

@Composable
fun PerfilScreen(onNotificaciones: () -> Unit, onLoggedOut: () -> Unit) {
    val vm: PerfilViewModel = viewModel(factory = appFactory { PerfilViewModel(it.auth) })
    val ocupado by vm.ocupado.collectAsStateWithLifecycle()
    val salio by vm.salio.collectAsStateWithLifecycle()

    LaunchedEffect(salio) {
        if (salio) onLoggedOut()
    }

    PerfilContent(
        usuario = vm.usuario,
        ocupado = ocupado,
        onNotificaciones = onNotificaciones,
        onCerrarSesion = vm::cerrarSesion,
    )
}

@Composable
fun PerfilContent(
    usuario: SessionUser?,
    ocupado: Boolean = false,
    onNotificaciones: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
) {
    var confirmar by remember { mutableStateOf(false) }

    AppScaffold(title = "Perfil") { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(usuario?.nombre.orEmpty(), style = MaterialTheme.typography.headlineSmall)
            Text(usuario?.email.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            Text("Rol: ${usuario?.rol.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onNotificaciones, modifier = Modifier.fillMaxWidth()) {
                Text("Notificaciones")
            }
            OutlinedButton(
                onClick = { confirmar = true },
                enabled = !ocupado,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cerrar sesión")
            }
        }
    }

    if (confirmar) {
        ConfirmDialog(
            title = "Cerrar sesión",
            text = "¿Seguro que deseas salir de la aplicación?",
            confirmText = "Salir",
            onConfirm = {
                confirmar = false
                onCerrarSesion()
            },
            onDismiss = { confirmar = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PerfilScreenPreview() {
    PetCareTheme {
        PerfilContent(
            usuario = SessionUser(
                id = 1,
                nombre = "Dr. Carlos Rodríguez",
                email = "carlos@veterinaria.hn",
                rol = "VETERINARIO",
            ),
        )
    }
}
