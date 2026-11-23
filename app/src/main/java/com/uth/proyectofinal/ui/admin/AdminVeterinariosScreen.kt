package com.uth.proyectofinal.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Veterinario
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.CatalogRepository
import com.uth.proyectofinal.ui.common.AddFab
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.EditIcon
import com.uth.proyectofinal.ui.common.EmptyBox
import com.uth.proyectofinal.ui.common.ErrorBox
import com.uth.proyectofinal.ui.common.LoadingBox
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.appFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VeterinariosUi(
    val items: List<Veterinario> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null,
)

class AdminVeterinariosViewModel(private val catalog: CatalogRepository) : ViewModel() {

    private val _ui = MutableStateFlow(VeterinariosUi())
    val ui: StateFlow<VeterinariosUi> = _ui.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            when (val result = catalog.veterinarios()) {
                is ApiResult.Success -> _ui.update { it.copy(items = result.data, cargando = false) }
                is ApiResult.Error -> _ui.update { it.copy(cargando = false, error = result.message) }
            }
        }
    }
}

@Composable
fun AdminVeterinariosScreen(onAdd: () -> Unit, onEdit: (Int) -> Unit, onHorarios: (Int) -> Unit) {
    val vm: AdminVeterinariosViewModel = viewModel(factory = appFactory { AdminVeterinariosViewModel(it.catalog) })
    val ui by vm.ui.collectAsStateWithLifecycle()

    OnResume { vm.cargar() }

    AppScaffold(
        title = "Veterinarios",
        floatingActionButton = { AddFab("Nuevo veterinario", onAdd) },
    ) { padding ->
        when {
            ui.cargando && ui.items.isEmpty() -> LoadingBox(Modifier.padding(padding))
            ui.error != null && ui.items.isEmpty() -> ErrorBox(ui.error!!, vm::cargar, Modifier.padding(padding))
            ui.items.isEmpty() -> EmptyBox("No hay veterinarios registrados.", Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ui.items, key = { it.id }) { v ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(v.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${v.email.orEmpty()} · ${v.horarios.size} día(s) con horario",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            IconButton(onClick = { onHorarios(v.id) }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Horarios")
                            }
                            EditIcon("Editar veterinario") { onEdit(v.id) }
                        }
                    }
                }
            }
        }
    }
}
