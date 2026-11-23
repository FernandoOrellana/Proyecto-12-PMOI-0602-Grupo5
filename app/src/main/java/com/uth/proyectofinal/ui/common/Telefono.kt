@file:OptIn(ExperimentalMaterial3Api::class)

package com.uth.proyectofinal.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

data class Pais(val nombre: String, val codigo: String, val grupos: List<Int>) {
    val digitos: Int get() = grupos.sum()
}

val PAISES = listOf(
    Pais("Honduras", "+504", listOf(4, 4)),
    Pais("Guatemala", "+502", listOf(4, 4)),
    Pais("El Salvador", "+503", listOf(4, 4)),
    Pais("Nicaragua", "+505", listOf(4, 4)),
    Pais("Costa Rica", "+506", listOf(4, 4)),
    Pais("Panamá", "+507", listOf(4, 4)),
    Pais("México", "+52", listOf(2, 4, 4)),
    Pais("Estados Unidos", "+1", listOf(3, 3, 4)),
)

fun formatearTelefono(digitos: String, grupos: List<Int>): String {
    val partes = mutableListOf<String>()
    var inicio = 0
    for (g in grupos) {
        if (inicio >= digitos.length) break
        val fin = minOf(inicio + g, digitos.length)
        partes += digitos.substring(inicio, fin)
        inicio = fin
    }
    return partes.joinToString("-")
}

class MascaraTelefono(private val grupos: List<Int>) : VisualTransformation {
    private val cortes = grupos.runningReduce { a, b -> a + b }.dropLast(1)

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val formateado = formatearTelefono(original, grupos)
        val mapeo = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                offset + cortes.count { it < offset && it < original.length }

            override fun transformedToOriginal(offset: Int): Int {
                for (o in 0..original.length) {
                    if (originalToTransformed(o) >= offset) return o
                }
                return original.length
            }
        }
        return TransformedText(AnnotatedString(formateado), mapeo)
    }
}

fun separarTelefono(telefono: String): Pair<Pais, String> {
    val limpio = telefono.trim()
    val pais = PAISES.sortedByDescending { it.codigo.length }.firstOrNull { limpio.startsWith(it.codigo) } ?: PAISES.first()
    val resto = if (limpio.startsWith(pais.codigo)) limpio.removePrefix(pais.codigo) else limpio
    return pais to resto.filter { it.isDigit() }.take(pais.digitos)
}

fun Pais.componer(numero: String): String =
    if (numero.isEmpty()) "" else "$codigo ${formatearTelefono(numero, grupos)}"

@Composable
fun CampoTelefono(
    pais: Pais,
    numero: String,
    onPais: (Pais) -> Unit,
    onNumero: (String) -> Unit,
    etiqueta: String = "Teléfono o WhatsApp",
) {
    var abierto by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        ExposedDropdownMenuBox(
            expanded = abierto,
            onExpandedChange = { abierto = it },
            modifier = Modifier.width(124.dp),
        ) {
            OutlinedTextField(
                value = pais.codigo,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Código") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                PAISES.forEach { p ->
                    DropdownMenuItem(
                        text = { Text("${p.nombre} (${p.codigo})") },
                        onClick = {
                            onPais(p)
                            abierto = false
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = numero,
            onValueChange = onNumero,
            label = { Text(etiqueta) },
            placeholder = { Text(formatearTelefono("0".repeat(pais.digitos), pais.grupos)) },
            singleLine = true,
            visualTransformation = remember(pais) { MascaraTelefono(pais.grupos) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}

fun normalizarTelefono(telefono: String): String {
    val digitos = telefono.filter { it.isDigit() }
    return when {
        telefono.trim().startsWith("+") -> digitos
        digitos.length == 8 -> "504$digitos"
        else -> digitos
    }
}

@Composable
fun ContactoCliente(nombre: String, telefono: String?, mensaje: String) {
    val context = LocalContext.current
    if (telefono.isNullOrBlank()) {
        Text(
            "$nombre no registró teléfono.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            telefono,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = { context.llamar("+" + normalizarTelefono(telefono)) },
            shape = MaterialTheme.shapes.small,
        ) { Text("Llamar") }
        Button(
            onClick = { context.escribirWhatsApp(normalizarTelefono(telefono), mensaje) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.padding(start = 8.dp),
        ) { Text("WhatsApp") }
    }
}
