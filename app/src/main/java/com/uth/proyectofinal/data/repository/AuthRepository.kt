package com.uth.proyectofinal.data.repository

import com.uth.proyectofinal.data.local.SessionUser
import com.uth.proyectofinal.data.local.TokenStore
import com.uth.proyectofinal.data.remote.ApiService
import com.uth.proyectofinal.data.remote.dto.LoginRequest
import com.uth.proyectofinal.data.remote.dto.RefreshRequest
import com.uth.proyectofinal.data.remote.dto.EmailRequest
import com.uth.proyectofinal.data.remote.dto.RegisterRequest
import com.uth.proyectofinal.data.remote.dto.ResetPasswordRequest
import com.uth.proyectofinal.data.remote.dto.Usuario
import com.uth.proyectofinal.data.remote.dto.VerifyEmailRequest
import kotlinx.coroutines.flow.StateFlow

class AuthRepository(private val api: ApiService, private val tokens: TokenStore) {

    val user: SessionUser? get() = tokens.user

    val sesion: StateFlow<SessionUser?> = tokens.sesion

    val hasSession: Boolean get() = tokens.accessToken != null && tokens.user != null

    suspend fun register(nombre: String, email: String, password: String, telefono: String?) =
        apiCall { api.register(RegisterRequest(nombre, email, password, telefono)) }.map { }

    suspend fun verifyEmail(email: String, codigo: String) =
        apiCall { api.verifyEmail(VerifyEmailRequest(email, codigo)) }.map { }

    suspend fun resendCode(email: String) =
        apiCall { api.resendCode(EmailRequest(email)) }.map { }

    suspend fun forgotPassword(email: String) =
        apiCall { api.forgotPassword(EmailRequest(email)) }.map { }

    suspend fun resetPassword(email: String, claveTemporal: String, nuevaPassword: String) =
        apiCall { api.resetPassword(ResetPasswordRequest(email, claveTemporal, nuevaPassword)) }.map { }

    suspend fun login(email: String, password: String): ApiResult<Usuario> {
        val result = apiCall { api.login(LoginRequest(email, password)) }
        if (result is ApiResult.Success) {
            tokens.saveTokens(result.data.accessToken, result.data.refreshToken)
            tokens.saveUser(result.data.usuario)
        }
        return result.map { it.usuario }
    }

    suspend fun logout() {
        val refresh = tokens.refreshToken
        if (refresh != null) runCatching { api.logout(RefreshRequest(refresh)) }
        tokens.clear()
    }
}
