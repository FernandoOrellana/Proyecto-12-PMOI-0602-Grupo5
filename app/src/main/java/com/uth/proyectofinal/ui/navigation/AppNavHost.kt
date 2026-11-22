package com.uth.proyectofinal.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uth.proyectofinal.AppContainer
import com.uth.proyectofinal.data.local.SessionEvents
import com.uth.proyectofinal.push.PushEvents
import com.uth.proyectofinal.ui.admin.AdminProductosScreen
import com.uth.proyectofinal.ui.admin.AdminPedidosScreen
import com.uth.proyectofinal.ui.admin.AdminPedidoDetailScreen
import com.uth.proyectofinal.ui.admin.AdminCitasScreen
import com.uth.proyectofinal.ui.admin.AdminCitaCobroScreen
import com.uth.proyectofinal.data.remote.dto.Rol
import com.uth.proyectofinal.ui.admin.AdminServiciosScreen
import com.uth.proyectofinal.ui.admin.AdminVeterinariosScreen
import com.uth.proyectofinal.ui.admin.HorariosScreen
import com.uth.proyectofinal.ui.admin.ProductoFormScreen
import com.uth.proyectofinal.ui.admin.ReportesScreen
import com.uth.proyectofinal.ui.admin.VeterinarioFormScreen
import com.uth.proyectofinal.ui.auth.ForgotPasswordScreen
import com.uth.proyectofinal.ui.auth.LoginScreen
import com.uth.proyectofinal.ui.auth.RegisterScreen
import com.uth.proyectofinal.ui.auth.ResetPasswordScreen
import com.uth.proyectofinal.ui.auth.SplashScreen
import com.uth.proyectofinal.ui.auth.VerifyEmailScreen
import com.uth.proyectofinal.ui.dueno.AgendarCitaScreen
import com.uth.proyectofinal.ui.dueno.CarneHistorialScreen
import com.uth.proyectofinal.ui.dueno.CarritoScreen
import com.uth.proyectofinal.ui.dueno.CitasScreen
import com.uth.proyectofinal.ui.dueno.MascotaDetailScreen
import com.uth.proyectofinal.ui.dueno.MascotaFormScreen
import com.uth.proyectofinal.ui.dueno.MascotasScreen
import com.uth.proyectofinal.ui.dueno.PedidoDetailScreen
import com.uth.proyectofinal.ui.dueno.PedidosScreen
import com.uth.proyectofinal.ui.dueno.TiendaScreen
import com.uth.proyectofinal.ui.mascota.HistorialCitasMascotaScreen
import com.uth.proyectofinal.ui.perfil.NotificacionesScreen
import com.uth.proyectofinal.ui.perfil.PerfilScreen
import com.uth.proyectofinal.ui.vet.AgendaScreen
import com.uth.proyectofinal.ui.vet.CitaDetailScreen
import com.uth.proyectofinal.ui.vet.ConsultaFormScreen
import com.uth.proyectofinal.ui.vet.VacunaFormScreen

@Composable
fun AppNavHost(container: AppContainer) {
    val usuario by container.auth.sesion.collectAsStateWithLifecycle()
    var arranque by rememberSaveable { mutableStateOf(true) }
    val claveSesion = usuario?.let { "${it.id}-${it.rol}" }

    LaunchedEffect(claveSesion) {
        if (claveSesion == null) container.cart.clear()
    }

    key(claveSesion) {
        val rol = usuario?.rol
        val inicio = remember {
            when {
                arranque -> Routes.SPLASH
                rol != null -> Routes.homeFor(rol)
                else -> Routes.LOGIN
            }
        }
        LaunchedEffect(Unit) { arranque = false }
        SesionNavHost(container, rol, inicio)
    }
}

@Composable
private fun SesionNavHost(container: AppContainer, rol: String?, inicio: String) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val tabs = tabsFor(rol)
    val showBar = tabs.any { it.route == route }

    LaunchedEffect(Unit) {
        SessionEvents.logout.collect {
            nav.navigate(Routes.LOGIN) {
                popUpTo(nav.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val abrirNotificaciones by PushEvents.pendiente.collectAsStateWithLifecycle()
    LaunchedEffect(abrirNotificaciones, route) {
        val sesionLista = route != null && route != Routes.SPLASH && route != Routes.LOGIN && rol != null
        if (abrirNotificaciones && sesionLista) {
            PushEvents.consumir()
            if (route != Routes.NOTIFICACIONES) nav.navigate(Routes.NOTIFICACIONES) { launchSingleTop = true }
        }
    }

    fun goLogin() = nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }

    Scaffold(
        bottomBar = {
            if (showBar) {
                val barModifier = if (rol == Rol.ADMINISTRADOR) {
                    Modifier.horizontalScroll(rememberScrollState()).widthIn(min = (tabs.size * 80).dp)
                } else Modifier
                NavigationBar(
                    modifier = barModifier,
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, maxLines = 1) },
                            alwaysShowLabel = tabs.size <= 5,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = inicio,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 12 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { it / 12 } },
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(container.auth, container.push) { target ->
                    nav.navigate(target) { popUpTo(nav.graph.id) { inclusive = true } }
                }
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onRegister = { nav.navigate(Routes.REGISTER) },
                    onForgot = { nav.navigate(Routes.FORGOT) },
                    onVerify = { nav.navigate(Routes.verify(it)) },
                    onLoggedIn = { nuevoRol ->
                        nav.navigate(Routes.homeFor(nuevoRol)) { popUpTo(nav.graph.id) { inclusive = true } }
                    },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onBack = { nav.popBackStack() },
                    onRegistered = { nav.navigate(Routes.verify(it)) },
                )
            }
            composable(Routes.VERIFY, arguments = stringArgs("email")) {
                VerifyEmailScreen(
                    email = it.str("email"),
                    onBack = { nav.popBackStack() },
                    onVerified = {
                        nav.navigate(Routes.LOGIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                )
            }
            composable(Routes.FORGOT) {
                ForgotPasswordScreen(
                    onBack = { nav.popBackStack() },
                    onSent = { nav.navigate(Routes.reset(it)) },
                )
            }
            composable(Routes.RESET, arguments = stringArgs("email")) {
                ResetPasswordScreen(
                    email = it.str("email"),
                    onBack = { nav.popBackStack() },
                    onDone = {
                        nav.navigate(Routes.LOGIN) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                )
            }

            composable(Routes.MASCOTAS) {
                MascotasScreen(
                    rol = rol,
                    onAdd = { nav.navigate(Routes.mascotaForm()) },
                    onOpen = { nav.navigate(Routes.mascota(it)) },
                )
            }
            composable(Routes.MASCOTA, arguments = intArgs("id")) {
                MascotaDetailScreen(
                    id = it.int("id"),
                    rol = rol,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.mascotaForm(it)) },
                    onCarne = { nav.navigate(Routes.carne(it)) },
                    onHistorialCitas = { nav.navigate(Routes.historialCitas(it)) },
                    onDeleted = { nav.popBackStack() },
                )
            }
            composable(Routes.MASCOTA_FORM, arguments = intArgs("id")) {
                MascotaFormScreen(
                    id = it.int("id"),
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                )
            }
            composable(Routes.CARNE, arguments = intArgs("id")) {
                CarneHistorialScreen(id = it.int("id"), onBack = { nav.popBackStack() })
            }
            composable(Routes.HISTORIAL_CITAS, arguments = intArgs("id")) {
                HistorialCitasMascotaScreen(
                    mascotaId = it.int("id"),
                    rol = rol,
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.CITAS) {
                CitasScreen(onNueva = { nav.navigate(Routes.CITA_NUEVA) })
            }
            composable(Routes.CITA_NUEVA) {
                AgendarCitaScreen(onBack = { nav.popBackStack() }, onCreated = { nav.popBackStack() })
            }

            composable(Routes.TIENDA) {
                TiendaScreen(onCarrito = { nav.navigate(Routes.CARRITO) })
            }
            composable(Routes.CARRITO) {
                CarritoScreen(
                    onBack = { nav.popBackStack() },
                    onPedidoCreado = {
                        nav.navigate(Routes.PEDIDOS) { popUpTo(Routes.TIENDA) }
                    },
                )
            }
            composable(Routes.PEDIDOS) {
                PedidosScreen(onOpen = { nav.navigate(Routes.pedido(it)) })
            }
            composable(Routes.PEDIDO, arguments = intArgs("id")) {
                PedidoDetailScreen(id = it.int("id"), onBack = { nav.popBackStack() })
            }

            composable(Routes.PERFIL) {
                PerfilScreen(
                    onNotificaciones = { nav.navigate(Routes.NOTIFICACIONES) },
                    onLoggedOut = { goLogin() },
                )
            }
            composable(Routes.NOTIFICACIONES) {
                NotificacionesScreen(onBack = { nav.popBackStack() })
            }

            composable(Routes.AGENDA) {
                AgendaScreen(onOpen = { nav.navigate(Routes.cita(it)) })
            }
            composable(Routes.CITA, arguments = intArgs("id")) {
                CitaDetailScreen(
                    id = it.int("id"),
                    onBack = { nav.popBackStack() },
                    onConsulta = { nav.navigate(Routes.consulta(it)) },
                    onVacuna = { citaId, mascotaId -> nav.navigate(Routes.vacuna(citaId, mascotaId)) },
                    onHistorialMascota = { nav.navigate(Routes.historialCitas(it)) },
                )
            }
            composable(Routes.CONSULTA, arguments = intArgs("citaId")) {
                ConsultaFormScreen(citaId = it.int("citaId"), onBack = { nav.popBackStack() })
            }
            composable(Routes.VACUNA, arguments = intArgs("citaId", "mascotaId")) {
                VacunaFormScreen(
                    citaId = it.int("citaId"),
                    mascotaId = it.int("mascotaId"),
                    onBack = { nav.popBackStack() },
                )
            }

            composable(Routes.ADMIN_PRODUCTOS) {
                AdminProductosScreen(
                    onAdd = { nav.navigate(Routes.productoForm()) },
                    onEdit = { nav.navigate(Routes.productoForm(it)) },
                )
            }
            composable(Routes.ADMIN_PEDIDOS) {
                AdminPedidosScreen(onOpen = { nav.navigate(Routes.adminPedido(it)) })
            }
            composable(Routes.ADMIN_PEDIDO, arguments = intArgs("id")) {
                AdminPedidoDetailScreen(id = it.int("id"), onBack = { nav.popBackStack() })
            }
            composable(Routes.ADMIN_CITAS) {
                AdminCitasScreen(onOpen = { nav.navigate(Routes.adminCita(it)) })
            }
            composable(Routes.ADMIN_CITA, arguments = intArgs("id")) {
                AdminCitaCobroScreen(id = it.int("id"), onBack = { nav.popBackStack() })
            }
            composable(Routes.PRODUCTO_FORM, arguments = intArgs("id")) {
                ProductoFormScreen(id = it.int("id"), onBack = { nav.popBackStack() })
            }
            composable(Routes.ADMIN_SERVICIOS) {
                AdminServiciosScreen()
            }
            composable(Routes.ADMIN_VETERINARIOS) {
                AdminVeterinariosScreen(
                    onAdd = { nav.navigate(Routes.veterinarioForm()) },
                    onEdit = { nav.navigate(Routes.veterinarioForm(it)) },
                    onHorarios = { nav.navigate(Routes.horarios(it)) },
                )
            }
            composable(Routes.VETERINARIO_FORM, arguments = intArgs("id")) {
                VeterinarioFormScreen(id = it.int("id"), onBack = { nav.popBackStack() })
            }
            composable(Routes.HORARIOS, arguments = intArgs("id")) {
                HorariosScreen(id = it.int("id"), onBack = { nav.popBackStack() })
            }
            composable(Routes.REPORTES) {
                ReportesScreen()
            }
        }
    }
}

private fun intArgs(vararg names: String): List<NamedNavArgument> =
    names.map { name -> navArgument(name) { type = NavType.IntType } }

private fun stringArgs(vararg names: String): List<NamedNavArgument> =
    names.map { name -> navArgument(name) { type = NavType.StringType } }

fun NavBackStackEntry.int(name: String): Int = arguments?.getInt(name) ?: 0

fun NavBackStackEntry.str(name: String): String = arguments?.getString(name).orEmpty()
