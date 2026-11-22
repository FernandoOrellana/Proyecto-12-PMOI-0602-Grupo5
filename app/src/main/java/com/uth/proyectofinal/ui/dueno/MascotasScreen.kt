package com.uth.proyectofinal.ui.dueno

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uth.proyectofinal.data.remote.dto.Mascota
import com.uth.proyectofinal.data.remote.dto.Ref
import com.uth.proyectofinal.data.remote.dto.Rol
import com.uth.proyectofinal.data.repository.ApiResult
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.data.repository.Page
import com.uth.proyectofinal.ui.common.AddFab
import com.uth.proyectofinal.ui.common.AppScaffold
import com.uth.proyectofinal.ui.common.OnResume
import com.uth.proyectofinal.ui.common.PagedColumn
import com.uth.proyectofinal.ui.common.PagedState
import com.uth.proyectofinal.ui.common.PagedViewModel
import com.uth.proyectofinal.ui.common.SimpleCard
import com.uth.proyectofinal.ui.common.appFactory
import com.uth.proyectofinal.ui.theme.PetCareTheme

class MascotasViewModel(private val repo: MascotaRepository) : PagedViewModel<Mascota>() {
    override suspend fun fetch(page: Int): ApiResult<Page<List<Mascota>>> = repo.list(page)
}

@Composable
fun MascotasScreen(
    rol: String? = null,
    onAdd: () -> Unit,
    onOpen: (Int) -> Unit,
) {
    val vm: MascotasViewModel = viewModel(factory = appFactory { MascotasViewModel(it.mascotas) })
    val state by vm.state.collectAsStateWithLifecycle()

    OnResume { vm.refresh() }

    MascotasContent(
        state = state,
        rol = rol,
        onAdd = onAdd,
        onOpen = onOpen,
        onLoadMore = vm::loadMore,
        onRefresh = vm::pullRefresh,
    )
}

@Composable
fun MascotasContent(
    state: PagedState<Mascota>,
    rol: String? = null,
    onAdd: () -> Unit = {},
    onOpen: (Int) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onRefresh: (() -> Unit)? = null,
) {
    val esDueno = (rol == null || rol == Rol.DUENO)

    AppScaffold(
        title = if (esDueno) "Mis mascotas" else "Mascotas / Pacientes",
        floatingActionButton = {
            if (esDueno) {
                AddFab("Agregar mascota", onAdd)
            }
        },
    ) { padding ->
        PagedColumn(
            state = state,
            emptyText = if (esDueno) {
                "Aún no tienes mascotas registradas. Usa + para agregar una."
            } else {
                "No hay mascotas registradas en el sistema."
            },
            onLoadMore = onLoadMore,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding),
        ) { m ->
            SimpleCard(
                title = m.nombre,
                subtitle = listOfNotNull(m.especie?.nombre, m.raza).joinToString(" · ").ifEmpty { null },
                onClick = { onOpen(m.id) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MascotasScreenPreview() {
    PetCareTheme {
        MascotasContent(
            state = PagedState(
                items = listOf(
                    Mascota(id = 1, nombre = "Max", raza = "Golden Retriever", especie = Ref(1, "Perro")),
                    Mascota(id = 2, nombre = "Luna", raza = "Siamés", especie = Ref(2, "Gato")),
                ),
            ),
        )
    }
}
