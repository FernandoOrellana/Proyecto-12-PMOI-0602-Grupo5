package com.uth.proyectofinal.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uth.proyectofinal.data.repository.AuthRepository
import com.uth.proyectofinal.push.PushRegistrar
import com.uth.proyectofinal.ui.navigation.Routes
import com.uth.proyectofinal.ui.theme.PetCareTheme

@Composable
fun SplashScreen(auth: AuthRepository, push: PushRegistrar, onReady: (String) -> Unit) {
    LaunchedEffect(Unit) {
        val user = auth.user
        if (auth.hasSession && user != null) {
            push.sync()
            onReady(Routes.homeFor(user.rol))
        } else {
            onReady(Routes.LOGIN)
        }
    }
    SplashContent()
}

@Composable
fun SplashContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("PetCare HN", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    PetCareTheme {
        SplashContent()
    }
}
