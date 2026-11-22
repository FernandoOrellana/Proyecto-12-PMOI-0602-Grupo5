package com.uth.proyectofinal.data.remote

import com.uth.proyectofinal.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<MessageDto>

    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): ApiResponse<MessageDto>

    @POST("api/auth/resend-code")
    suspend fun resendCode(@Body body: EmailRequest): ApiResponse<MessageDto>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<LoginResponse>

    @POST("api/auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<ApiResponse<TokensDto>>

    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: EmailRequest): ApiResponse<MessageDto>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): ApiResponse<MessageDto>

    @POST("api/auth/logout")
    suspend fun logout(@Body body: RefreshRequest): ApiResponse<MessageDto>

    @POST("api/usuarios/me/dispositivos")
    suspend fun registrarDispositivo(@Body body: DeviceRequest): ApiResponse<MessageDto>

    @GET("api/especies")
    suspend fun especies(): ApiResponse<List<Especie>>

    @GET("api/vacunas")
    suspend fun vacunas(): ApiResponse<List<Vacuna>>

    @GET("api/servicios")
    suspend fun servicios(): ApiResponse<List<Servicio>>

    @GET("api/veterinarios")
    suspend fun veterinarios(): ApiResponse<List<Veterinario>>

    @GET("api/veterinarios/{id}/disponibilidad")
    suspend fun disponibilidad(
        @Path("id") id: Int,
        @Query("fecha") fecha: String,
    ): ApiResponse<Disponibilidad>

    @GET("api/mascotas")
    suspend fun mascotas(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<Mascota>>

    @POST("api/mascotas")
    suspend fun crearMascota(@Body body: MascotaRequest): ApiResponse<Mascota>

    @GET("api/mascotas/{id}")
    suspend fun mascota(@Path("id") id: Int): ApiResponse<Mascota>

    @PUT("api/mascotas/{id}")
    suspend fun editarMascota(@Path("id") id: Int, @Body body: MascotaRequest): ApiResponse<Mascota>

    @DELETE("api/mascotas/{id}")
    suspend fun eliminarMascota(@Path("id") id: Int): ApiResponse<MessageDto>

    @Multipart
    @POST("api/mascotas/{id}/foto")
    suspend fun subirFotoMascota(
        @Path("id") id: Int,
        @Part foto: MultipartBody.Part,
    ): ApiResponse<Mascota>

    @GET("api/mascotas/{id}/carne")
    suspend fun carne(@Path("id") id: Int): ApiResponse<Carne>

    @GET("api/mascotas/{id}/historial")
    suspend fun historial(@Path("id") id: Int): ApiResponse<Historial>

    @GET("api/mascotas/{id}/citas")
    suspend fun citasMascota(@Path("id") id: Int): ApiResponse<List<Cita>>

    @POST("api/mascotas/{id}/vacunas")
    suspend fun registrarVacuna(
        @Path("id") id: Int,
        @Body body: VacunaRequest,
    ): ApiResponse<AplicacionVacuna>

    @POST("api/citas")
    suspend fun crearCita(@Body body: CitaRequest): ApiResponse<Cita>

    @GET("api/citas")
    suspend fun citas(
        @Query("fecha") fecha: String?,
        @Query("estado") estado: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<Cita>>

    @GET("api/citas/{id}")
    suspend fun cita(@Path("id") id: Int): ApiResponse<Cita>

    @PATCH("api/citas/{id}/cancelar")
    suspend fun cancelarCita(@Path("id") id: Int): ApiResponse<Cita>

    @PATCH("api/citas/{id}/iniciar")
    suspend fun iniciarConsulta(@Path("id") id: Int): ApiResponse<Cita>

    @POST("api/citas/{id}/consulta")
    suspend fun registrarConsulta(
        @Path("id") id: Int,
        @Body body: ConsultaRequest,
    ): ApiResponse<Consulta>

    @PATCH("api/citas/{id}/observaciones")
    suspend fun agregarObservacion(
        @Path("id") id: Int,
        @Body body: com.uth.proyectofinal.data.remote.dto.ObservacionesRequest,
    ): ApiResponse<Consulta>

    @GET("api/productos")
    suspend fun productos(
        @Query("categoria") categoria: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<Producto>>

    @GET("api/productos/{id}")
    suspend fun producto(@Path("id") id: Int): ApiResponse<Producto>

    @POST("api/pedidos")
    suspend fun crearPedido(@Body body: PedidoRequest): ApiResponse<Pedido>

    @GET("api/pedidos")
    suspend fun pedidos(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): ApiResponse<List<Pedido>>

    @GET("api/pedidos/{id}")
    suspend fun pedido(@Path("id") id: Int): ApiResponse<Pedido>

    @PATCH("api/pedidos/{id}/estado")
    suspend fun cambiarEstadoPedido(@Path("id") id: Int, @Body body: EstadoPedidoRequest): ApiResponse<Pedido>

    @GET("api/admin/pedidos/{id}/cobro")
    suspend fun cobroPedido(@Path("id") id: Int): Response<ApiResponse<ComprobanteInterno>>

    @POST("api/admin/pedidos/{id}/cobro")
    suspend fun cobrarPedido(@Path("id") id: Int, @Body body: RegistrarCobroRequest): ApiResponse<ComprobanteInterno>

    @GET("api/admin/citas/{id}/cobro")
    suspend fun cobroCita(@Path("id") id: Int): Response<ApiResponse<ComprobanteInterno>>

    @POST("api/admin/citas/{id}/cobro")
    suspend fun cobrarCita(@Path("id") id: Int, @Body body: RegistrarCobroRequest): ApiResponse<ComprobanteInterno>

    @GET("api/notificaciones")
    suspend fun notificaciones(): ApiResponse<List<Notificacion>>

    @POST("api/notificaciones/prueba")
    suspend fun probarNotificacion(): ApiResponse<PruebaPushDto>

    @PATCH("api/notificaciones/{id}/leer")
    suspend fun leerNotificacion(@Path("id") id: Int): ApiResponse<Notificacion>

    @POST("api/admin/productos")
    suspend fun crearProducto(@Body body: ProductoRequest): ApiResponse<Producto>

    @PUT("api/admin/productos/{id}")
    suspend fun editarProducto(
        @Path("id") id: Int,
        @Body body: ProductoRequest,
    ): ApiResponse<Producto>

    @DELETE("api/admin/productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: Int): ApiResponse<MessageDto>

    @Multipart
    @POST("api/admin/productos/{id}/imagen")
    suspend fun subirImagenProducto(
        @Path("id") id: Int,
        @Part imagen: MultipartBody.Part,
    ): ApiResponse<Producto>

    @POST("api/admin/servicios")
    suspend fun crearServicio(@Body body: ServicioRequest): ApiResponse<Servicio>

    @PUT("api/admin/servicios/{id}")
    suspend fun editarServicio(
        @Path("id") id: Int,
        @Body body: ServicioRequest,
    ): ApiResponse<Servicio>

    @DELETE("api/admin/servicios/{id}")
    suspend fun eliminarServicio(@Path("id") id: Int): ApiResponse<MessageDto>

    @POST("api/admin/veterinarios")
    suspend fun crearVeterinario(@Body body: CrearVeterinarioRequest): ApiResponse<Veterinario>

    @PUT("api/admin/veterinarios/{id}")
    suspend fun editarVeterinario(
        @Path("id") id: Int,
        @Body body: EditarVeterinarioRequest,
    ): ApiResponse<Veterinario>

    @PUT("api/admin/veterinarios/{id}/horarios")
    suspend fun guardarHorarios(
        @Path("id") id: Int,
        @Body body: List<Horario>,
    ): ApiResponse<Veterinario>

    @GET("api/admin/reportes/ventas")
    suspend fun reporteVentas(
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
    ): ApiResponse<ReporteVentas>

    @GET("api/admin/reportes/citas")
    suspend fun reporteCitas(
        @Query("desde") desde: String,
        @Query("hasta") hasta: String,
    ): ApiResponse<ReporteCitas>
}
