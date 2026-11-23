package com.uth.proyectofinal.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun VeterinariaCard() {
    val context = LocalContext.current
    val clinica = datosClinica()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CirculoIcono(Icons.Default.Place)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(clinica.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(
                        clinica.direccion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (clinica.horario.isNotBlank()) {
                        Text(clinica.horario, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { context.abrirMapa(clinica.lugar()) },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cómo llegar")
                }
                if (clinica.telefono.isNotBlank()) {
                    OutlinedButton(onClick = { context.llamar(clinica.telefono) }, shape = MaterialTheme.shapes.small) {
                        Icon(Icons.Default.Call, contentDescription = "Llamar", modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(onClick = { context.escribirWhatsApp(clinica.telefono) }, shape = MaterialTheme.shapes.small) {
                        Icon(Icons.Default.Email, contentDescription = "WhatsApp", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
