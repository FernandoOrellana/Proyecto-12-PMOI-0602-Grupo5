package com.uth.proyectofinal.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.uth.proyectofinal.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val HUELLAS = 12

@Composable
fun EspiralEntrada(modifier: Modifier = Modifier) {
    val giro = remember { Animatable(0f) }
    val escala = remember { Animatable(0.7f) }
    val avance = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { giro.animateTo(1080f, tween(1350, easing = FastOutSlowInEasing)) }
        launch {
            escala.animateTo(1.15f, tween(420))
            escala.animateTo(0f, tween(930, easing = FastOutSlowInEasing))
        }
        launch {
            delay(180)
            avance.animateTo(1f, tween(1150, easing = LinearOutSlowInEasing))
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val p = avance.value
        repeat(HUELLAS) { i ->
            val base = i * (360f / HUELLAS)
            val radioDp = 10f + p * (70f + (i % 4) * 20f)
            val grados = base + p * 160f
            val radianes = grados * PI.toFloat() / 180f
            Icon(
                painter = painterResource(R.drawable.ic_notificacion),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        val radio = radioDp.dp.toPx()
                        translationX = cos(radianes) * radio
                        translationY = sin(radianes) * radio
                        rotationZ = grados
                        alpha = sin(PI.toFloat() * p)
                        scaleX = 0.5f + p * 0.6f
                        scaleY = 0.5f + p * 0.6f
                    },
            )
        }
        Box(
            Modifier
                .size(104.dp)
                .graphicsLayer {
                    rotationZ = giro.value
                    scaleX = escala.value
                    scaleY = escala.value
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
