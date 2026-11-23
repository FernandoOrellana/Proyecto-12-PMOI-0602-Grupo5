package com.uth.proyectofinal.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uth.proyectofinal.PetCareApp
import com.uth.proyectofinal.data.local.DatosClinica
import java.text.SimpleDateFormat
import java.util.Locale

private fun Context.abrir(intent: Intent, error: String) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }
}

fun Context.abrirMapa(consulta: String) {
    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(consulta)}")
    abrir(Intent(Intent.ACTION_VIEW, uri), "No hay una app de mapas instalada.")
}

fun Context.llamar(telefono: String) {
    abrir(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + telefono.filter { it.isDigit() || it == '+' })), "No se pudo abrir el teléfono.")
}

fun Context.escribirWhatsApp(telefono: String, mensaje: String = "Hola, quiero información sobre mi mascota.") {
    val numero = normalizarTelefono(telefono)
    val uri = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
    abrir(Intent(Intent.ACTION_VIEW, uri), "No se pudo abrir WhatsApp.")
}

fun Context.agregarAlCalendario(titulo: String, descripcion: String, fecha: String, hora: String, lugar: String, minutos: Int = 30) {
    val inicio = runCatching {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).parse("${fecha.take(10)} ${hora.take(5)}")?.time
    }.getOrNull()
    if (inicio == null) {
        Toast.makeText(this, "La fecha de la cita no es válida.", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicio)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, inicio + minutos * 60_000L)
        .putExtra(CalendarContract.Events.TITLE, titulo)
        .putExtra(CalendarContract.Events.DESCRIPTION, descripcion)
        .putExtra(CalendarContract.Events.EVENT_LOCATION, lugar)
        .putExtra(CalendarContract.Events.HAS_ALARM, 1)
    abrir(intent, "No hay una app de calendario instalada.")
}

@Composable
fun datosClinica(): DatosClinica {
    val app = LocalContext.current.applicationContext as PetCareApp
    val datos by app.container.clinica.datos.collectAsStateWithLifecycle()
    return datos
}

fun DatosClinica.lugar(): String = listOf(nombre, direccion).filter { it.isNotBlank() }.joinToString(", ")
