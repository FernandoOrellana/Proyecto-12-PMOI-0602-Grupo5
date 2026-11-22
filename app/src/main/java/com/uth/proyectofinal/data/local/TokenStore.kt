package com.uth.proyectofinal.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.uth.proyectofinal.data.remote.dto.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionUser(val id: Int, val nombre: String, val email: String, val rol: String)

@Suppress("DEPRECATION")
class TokenStore(context: Context) {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "petcare_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)

    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)

    private val _sesion = MutableStateFlow(leerUsuario())
    val sesion: StateFlow<SessionUser?> = _sesion.asStateFlow()

    val user: SessionUser?
        get() = leerUsuario()

    private fun leerUsuario(): SessionUser? {
        val id = prefs.getInt(KEY_USER_ID, 0)
        val rol = prefs.getString(KEY_ROL, null)
        if (id == 0 || rol == null) return null
        return SessionUser(
            id = id,
            nombre = prefs.getString(KEY_NOMBRE, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            rol = rol,
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_REFRESH, refreshToken)
        }
    }

    fun saveUser(usuario: Usuario) {
        prefs.edit {
            putInt(KEY_USER_ID, usuario.id)
            putString(KEY_NOMBRE, usuario.nombre)
            putString(KEY_EMAIL, usuario.email)
            putString(KEY_ROL, usuario.rol)
        }
        _sesion.value = leerUsuario()
    }

    fun clear() {
        prefs.edit { clear() }
        _sesion.value = null
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_NOMBRE = "user_nombre"
        const val KEY_EMAIL = "user_email"
        const val KEY_ROL = "user_rol"
    }
}
