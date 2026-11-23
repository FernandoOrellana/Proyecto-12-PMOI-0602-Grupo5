package com.uth.proyectofinal.ui.admin

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.uth.proyectofinal.data.remote.dto.ReporteCitas
import com.uth.proyectofinal.data.remote.dto.ReporteVentas
import com.uth.proyectofinal.ui.common.autoridadArchivos
import com.uth.proyectofinal.ui.common.lps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ANCHO = 595
private const val ALTO = 842
private const val MARGEN = 40f
private const val PIE = 60f
private val VERDE = Color.rgb(27, 127, 92)
private val VERDE_CLARO = Color.rgb(213, 242, 229)
private val GRIS = Color.rgb(91, 104, 97)
private val GRIS_CLARO = Color.rgb(242, 245, 243)

private class Hoja {
    val documento = PdfDocument()
    private var numero = 0
    private var pagina: PdfDocument.Page? = null
    lateinit var canvas: Canvas
    var y = MARGEN

    val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; color = VERDE }
    val negrita = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD; color = Color.BLACK }
    val normal = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = Color.BLACK }
    val tenue = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8.5f; color = GRIS }
    val relleno = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun nuevaPagina() {
        cerrarPagina()
        numero++
        pagina = documento.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero).create())
        canvas = pagina!!.canvas
        y = MARGEN
    }

    fun cerrarPagina() {
        pagina?.let {
            canvas.drawText("PetCare HN · Veterinaria Huellitas · Página $numero", MARGEN, ALTO - 25f, tenue)
            documento.finishPage(it)
        }
        pagina = null
    }

    fun espacio(alto: Float) {
        if (y + alto > ALTO - PIE) nuevaPagina()
    }

    fun recortar(texto: String, ancho: Float, paint: Paint): String {
        if (paint.measureText(texto) <= ancho) return texto
        var fin = texto.length
        while (fin > 0 && paint.measureText(texto.substring(0, fin) + "…") > ancho) fin--
        return texto.substring(0, fin) + "…"
    }
}

suspend fun Context.generarReportePdf(desde: String, hasta: String, ventas: ReporteVentas?, citas: ReporteCitas?): File =
    withContext(Dispatchers.IO) {
        val h = Hoja()
        h.nuevaPagina()
        encabezado(h, desde, hasta)
        resumen(h, ventas, citas)
        productos(h, ventas)
        barras(h, "Citas por servicio", citas?.porServicio.orEmpty().map { it.nombre to it.cantidad })
        tablaEstados(h, citas)
        h.cerrarPagina()

        val carpeta = File(cacheDir, "carnes").apply { mkdirs() }
        val archivo = File(carpeta, "reporte_${desde}_$hasta.pdf")
        archivo.outputStream().use { h.documento.writeTo(it) }
        h.documento.close()
        archivo
    }

fun Context.compartirReporte(archivo: File) {
    val uri = FileProvider.getUriForFile(this, autoridadArchivos(), archivo)
    val enviar = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Reporte PetCare HN")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(enviar, "Compartir reporte").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun encabezado(h: Hoja, desde: String, hasta: String) {
    h.relleno.color = VERDE
    h.canvas.drawRect(0f, 0f, ANCHO.toFloat(), 96f, h.relleno)
    val blanco = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    blanco.textSize = 20f
    blanco.typeface = Typeface.DEFAULT_BOLD
    h.canvas.drawText("Reporte de ventas y citas", MARGEN, 42f, blanco)
    blanco.textSize = 11f
    blanco.typeface = Typeface.DEFAULT
    h.canvas.drawText("Veterinaria Huellitas · Comayagüela · PetCare HN", MARGEN, 62f, blanco)
    val emitido = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    h.canvas.drawText("Periodo: $desde al $hasta · Emitido el $emitido", MARGEN, 80f, blanco)
    h.y = 120f
}

private fun seccion(h: Hoja, texto: String) {
    h.espacio(48f)
    h.canvas.drawText(texto, MARGEN, h.y, h.titulo)
    h.y += 8f
    h.relleno.color = VERDE
    h.canvas.drawRect(MARGEN, h.y, ANCHO - MARGEN, h.y + 1.5f, h.relleno)
    h.y += 18f
}

private fun resumen(h: Hoja, ventas: ReporteVentas?, citas: ReporteCitas?) {
    val datos = listOf(
        "Total vendido" to (ventas?.totalVentas ?: 0.0).lps(),
        "Pedidos" to "${ventas?.totalPedidos ?: 0}",
        "Citas" to "${citas?.total ?: 0}",
    )
    val ancho = (ANCHO - MARGEN * 2 - 20f) / datos.size
    val grande = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f; typeface = Typeface.DEFAULT_BOLD; color = VERDE }
    datos.forEachIndexed { i, (etiqueta, valor) ->
        val x = MARGEN + i * (ancho + 10f)
        h.relleno.color = VERDE_CLARO
        h.canvas.drawRoundRect(RectF(x, h.y, x + ancho, h.y + 58f), 10f, 10f, h.relleno)
        h.canvas.drawText(etiqueta, x + 12f, h.y + 20f, h.tenue)
        h.canvas.drawText(h.recortar(valor, ancho - 24f, grande), x + 12f, h.y + 44f, grande)
    }
    h.y += 84f
}

private fun fila(h: Hoja, columnas: List<Float>, textos: List<String>, paint: Paint, fondo: Int? = null) {
    h.espacio(20f)
    if (fondo != null) {
        h.relleno.color = fondo
        h.canvas.drawRect(MARGEN, h.y - 12f, ANCHO - MARGEN, h.y + 5f, h.relleno)
    }
    var x = MARGEN + 4f
    textos.forEachIndexed { i, t ->
        h.canvas.drawText(h.recortar(t, columnas[i] - 8f, paint), x, h.y, paint)
        x += columnas[i]
    }
    h.y += 18f
}

private fun productos(h: Hoja, ventas: ReporteVentas?) {
    seccion(h, "Productos más vendidos")
    val lista = ventas?.productos.orEmpty().sortedByDescending { it.total }
    if (lista.isEmpty()) {
        h.canvas.drawText("Sin ventas en el periodo.", MARGEN, h.y, h.tenue)
        h.y += 24f
        return
    }
    val columnas = listOf(300f, 90f, 125f)
    fila(h, columnas, listOf("Producto", "Cantidad", "Total"), h.negrita, VERDE_CLARO)
    lista.forEachIndexed { i, p ->
        fila(h, columnas, listOf(p.nombre, "${p.cantidad}", p.total.lps()), h.normal, if (i % 2 == 1) GRIS_CLARO else null)
    }
    h.y += 10f
}

private fun barras(h: Hoja, titulo: String, datos: List<Pair<String, Int>>) {
    seccion(h, titulo)
    if (datos.isEmpty()) {
        h.canvas.drawText("Sin citas en el periodo.", MARGEN, h.y, h.tenue)
        h.y += 24f
        return
    }
    val maximo = datos.maxOf { it.second }.coerceAtLeast(1)
    val etiqueta = 150f
    val anchoBarra = ANCHO - MARGEN * 2 - etiqueta - 40f
    datos.sortedByDescending { it.second }.forEach { (nombre, cantidad) ->
        h.espacio(22f)
        h.canvas.drawText(h.recortar(nombre, etiqueta - 10f, h.normal), MARGEN, h.y, h.normal)
        val largo = anchoBarra * cantidad / maximo
        h.relleno.color = GRIS_CLARO
        h.canvas.drawRoundRect(RectF(MARGEN + etiqueta, h.y - 10f, MARGEN + etiqueta + anchoBarra, h.y + 3f), 6f, 6f, h.relleno)
        h.relleno.color = VERDE
        h.canvas.drawRoundRect(RectF(MARGEN + etiqueta, h.y - 10f, MARGEN + etiqueta + largo.coerceAtLeast(6f), h.y + 3f), 6f, 6f, h.relleno)
        h.canvas.drawText("$cantidad", MARGEN + etiqueta + anchoBarra + 8f, h.y, h.negrita)
        h.y += 22f
    }
    h.y += 10f
}

private fun tablaEstados(h: Hoja, citas: ReporteCitas?) {
    val estados = citas?.porEstado.orEmpty()
    if (estados.isEmpty()) return
    seccion(h, "Citas por estado")
    val columnas = listOf(300f, 215f)
    fila(h, columnas, listOf("Estado", "Cantidad"), h.negrita, VERDE_CLARO)
    estados.entries.forEachIndexed { i, (estado, cantidad) ->
        fila(h, columnas, listOf(estado.replace('_', ' '), "$cantidad"), h.normal, if (i % 2 == 1) GRIS_CLARO else null)
    }
}
