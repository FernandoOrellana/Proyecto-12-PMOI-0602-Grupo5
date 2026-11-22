package com.uth.proyectofinal.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import com.uth.proyectofinal.data.remote.dto.Rol

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val VERIFY = "verify/{email}"
    const val FORGOT = "forgot"
    const val RESET = "reset/{email}"

    const val MASCOTAS = "mascotas"
    const val MASCOTA = "mascota/{id}"
    const val MASCOTA_FORM = "mascota_form/{id}"
    const val CARNE = "carne/{id}"
    const val HISTORIAL_CITAS = "mascota/{id}/citas"
    const val CITAS = "citas"
    const val CITA_NUEVA = "cita_nueva"
    const val TIENDA = "tienda"
    const val CARRITO = "carrito"
    const val PEDIDOS = "pedidos"
    const val PEDIDO = "pedido/{id}"
    const val PERFIL = "perfil"
    const val NOTIFICACIONES = "notificaciones"

    const val AGENDA = "agenda"
    const val CITA = "cita/{id}"
    const val CONSULTA = "consulta/{citaId}"
    const val VACUNA = "vacuna/{citaId}/{mascotaId}"

    const val ADMIN_PRODUCTOS = "admin_productos"
    const val ADMIN_PEDIDOS = "admin_pedidos"
    const val ADMIN_PEDIDO = "admin_pedido/{id}"
    const val ADMIN_CITAS = "admin_citas"
    const val ADMIN_CITA = "admin_cita/{id}"
    const val PRODUCTO_FORM = "producto_form/{id}"
    const val ADMIN_SERVICIOS = "admin_servicios"
    const val ADMIN_VETERINARIOS = "admin_veterinarios"
    const val VETERINARIO_FORM = "veterinario_form/{id}"
    const val HORARIOS = "horarios/{id}"
    const val REPORTES = "reportes"

    const val NUEVO = 0

    fun verify(email: String) = "verify/${Uri.encode(email)}"
    fun reset(email: String) = "reset/${Uri.encode(email)}"
    fun mascota(id: Int) = "mascota/$id"
    fun mascotaForm(id: Int = NUEVO) = "mascota_form/$id"
    fun carne(id: Int) = "carne/$id"
    fun historialCitas(id: Int) = "mascota/$id/citas"
    fun cita(id: Int) = "cita/$id"
    fun consulta(citaId: Int) = "consulta/$citaId"
    fun vacuna(citaId: Int, mascotaId: Int) = "vacuna/$citaId/$mascotaId"
    fun pedido(id: Int) = "pedido/$id"
    fun adminPedido(id: Int) = "admin_pedido/$id"
    fun adminCita(id: Int) = "admin_cita/$id"
    fun productoForm(id: Int = NUEVO) = "producto_form/$id"
    fun veterinarioForm(id: Int = NUEVO) = "veterinario_form/$id"
    fun horarios(id: Int) = "horarios/$id"

    fun homeFor(rol: String?): String = when (rol) {
        Rol.VETERINARIO -> AGENDA
        Rol.ADMINISTRADOR -> ADMIN_PRODUCTOS
        else -> MASCOTAS
    }
}

enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    MASCOTAS(Routes.MASCOTAS, "Mascotas", Icons.Default.Favorite),
    CITAS(Routes.CITAS, "Citas", Icons.Default.DateRange),
    TIENDA(Routes.TIENDA, "Tienda", Icons.Default.ShoppingCart),
    PEDIDOS(Routes.PEDIDOS, "Pedidos", Icons.AutoMirrored.Filled.List),
    AGENDA(Routes.AGENDA, "Agenda", Icons.Default.DateRange),
    PRODUCTOS(Routes.ADMIN_PRODUCTOS, "Productos", Icons.Default.ShoppingCart),
    ADMIN_PEDIDOS(Routes.ADMIN_PEDIDOS, "Pedidos", Icons.AutoMirrored.Filled.List),
    ADMIN_CITAS(Routes.ADMIN_CITAS, "Citas", Icons.Default.DateRange),
    SERVICIOS(Routes.ADMIN_SERVICIOS, "Servicios", Icons.Default.Settings),
    VETERINARIOS(Routes.ADMIN_VETERINARIOS, "Veterinarios", Icons.Default.Face),
    REPORTES(Routes.REPORTES, "Reportes", Icons.Default.Info),
    PERFIL(Routes.PERFIL, "Perfil", Icons.Default.Person),
}

fun tabsFor(rol: String?): List<MainTab> = when (rol) {
    Rol.VETERINARIO -> listOf(MainTab.AGENDA, MainTab.MASCOTAS, MainTab.PERFIL)
    Rol.ADMINISTRADOR -> listOf(
        MainTab.PRODUCTOS,
        MainTab.ADMIN_PEDIDOS,
        MainTab.ADMIN_CITAS,
        MainTab.SERVICIOS,
        MainTab.VETERINARIOS,
        MainTab.MASCOTAS,
        MainTab.REPORTES,
        MainTab.PERFIL,
    )
    else -> listOf(MainTab.MASCOTAS, MainTab.CITAS, MainTab.TIENDA, MainTab.PEDIDOS, MainTab.PERFIL)
}
