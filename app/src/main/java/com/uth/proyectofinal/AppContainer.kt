package com.uth.proyectofinal

import android.content.Context
import android.os.Build
import com.uth.proyectofinal.data.local.CartStore
import com.uth.proyectofinal.data.local.ClinicaStore
import com.uth.proyectofinal.data.local.SessionEvents
import com.uth.proyectofinal.data.local.TokenStore
import com.uth.proyectofinal.data.remote.ApiClient
import com.uth.proyectofinal.data.repository.AdminRepository
import com.uth.proyectofinal.data.repository.AuthRepository
import com.uth.proyectofinal.data.repository.CatalogRepository
import com.uth.proyectofinal.data.repository.CitaRepository
import com.uth.proyectofinal.data.repository.MascotaRepository
import com.uth.proyectofinal.data.repository.NotificacionRepository
import com.uth.proyectofinal.data.repository.TiendaRepository
import com.uth.proyectofinal.push.PushRegistrar

class AppContainer(context: Context) {
    val tokens = TokenStore(context)
    val cart = CartStore()
    val clinica = ClinicaStore(context)

    private val api = ApiClient(BuildConfig.API_BASE_URL, tokens) { SessionEvents.emitLogout() }.service

    val auth = AuthRepository(api, tokens)
    val catalog = CatalogRepository(api)
    val mascotas = MascotaRepository(api)
    val citas = CitaRepository(api)
    val tienda = TiendaRepository(api)
    val notificaciones = NotificacionRepository(api)
    val admin = AdminRepository(api)
    val push = PushRegistrar(notificaciones, tokens)
}
