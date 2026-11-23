package com.uth.proyectofinal.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val FORMATO_ISO = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val ESPANOL = Locale.forLanguageTag("es-HN")

fun fechaMasDias(dias: Int): String =
    isoFecha(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dias) })

fun diasHasta(fecha: String): Int? = runCatching {
    val hoy = FORMATO_ISO.parse(isoFecha())?.time ?: return null
    val destino = FORMATO_ISO.parse(fecha.take(10))?.time ?: return null
    ((destino - hoy) / 86_400_000L).toInt()
}.getOrNull()

fun diaSemanaDe(fecha: String): Int = runCatching {
    val date = FORMATO_ISO.parse(fecha.take(10)) ?: return -1
    Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK) - 1
}.getOrDefault(-1)

fun primerDiaLaboral(dias: Set<Int>): String =
    (0..30).map { fechaMasDias(it) }.firstOrNull { diaSemanaDe(it) in dias } ?: isoFecha()

fun nombreDia(fecha: String): String = runCatching {
    val date = FORMATO_ISO.parse(fecha.take(10)) ?: return ""
    SimpleDateFormat("EEE", ESPANOL).format(date).replace(".", "").replaceFirstChar { it.uppercase() }
}.getOrDefault("")

fun fechaCorta(fecha: String): String = runCatching {
    val date = FORMATO_ISO.parse(fecha.take(10)) ?: return fecha
    SimpleDateFormat("d MMM", ESPANOL).format(date).replace(".", "")
}.getOrDefault(fecha)

fun textoCuando(fecha: String): String {
    val d = diasHasta(fecha) ?: return fecha
    return when (d) {
        0 -> "Hoy"
        1 -> "Mañana"
        in 2..6 -> nombreDia(fecha)
        else -> fechaCorta(fecha)
    }
}

@Composable
fun DiasSelector(
    fecha: String,
    onFecha: (String) -> Unit,
    modifier: Modifier = Modifier,
    desde: Int = 0,
    dias: Int = 14,
    onOtraFecha: (() -> Unit)? = null,
    diasPermitidos: Set<Int>? = null,
) {
    val lista = (desde until desde + dias).map { fechaMasDias(it) }
        .filter { diasPermitidos == null || diaSemanaDe(it) in diasPermitidos }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(lista) { dia ->
            val elegido = dia == fecha
            Card(
                onClick = { onFecha(dia) },
                colors = CardDefaults.cardColors(
                    containerColor = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    contentColor = if (elegido) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Column(
                    Modifier.width(52.dp).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(nombreDia(dia), style = MaterialTheme.typography.labelSmall)
                    Text(dia.takeLast(2).trimStart('0'), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (onOtraFecha != null) {
            item {
                Card(onClick = onOtraFecha) {
                    Box(Modifier.size(width = 52.dp, height = 58.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DateRange, contentDescription = "Otra fecha", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun FiltroChips(
    opciones: List<String>,
    seleccion: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(opciones) { opcion ->
            FilterChip(
                selected = opcion == seleccion,
                onClick = { onSelect(opcion) },
                label = { Text(opcion) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
fun FotoCirculo(url: String?, nombre: String, size: Dp = 44.dp) {
    if (url.isNullOrBlank()) {
        Avatar(nombre, Modifier.size(size))
    } else {
        AsyncImage(
            model = url,
            contentDescription = nombre,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
    }
}

@Composable
fun AccesoRapido(icon: ImageVector, texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                texto,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MetricaCard(titulo: String, valor: String, detalle: String?, modifier: Modifier = Modifier, destacado: Boolean = false) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valor,
                style = MaterialTheme.typography.headlineSmall,
                color = if (destacado) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun FilaNavegable(titulo: String, subtitulo: String?, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
fun PasosBarra(actual: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (i <= actual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
fun OpcionCard(
    titulo: String,
    subtitulo: String?,
    seleccionada: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit = {},
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            leading()
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitulo != null) {
                    Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (seleccionada) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun CirculoIcono(icon: ImageVector, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size / 2))
    }
}

@Composable
fun Seccion(titulo: String, modifier: Modifier = Modifier, accion: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        accion?.invoke()
    }
}

@Composable
fun BarraSemanal(valores: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val maximo = (valores.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    Row(
        modifier.fillMaxWidth().height(110.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        valores.forEachIndexed { i, (etiqueta, valor) ->
            Column(Modifier.weight(1f).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text("$valor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((70f * valor / maximo).coerceAtLeast(4f).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (i == valores.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        ),
                )
                Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
