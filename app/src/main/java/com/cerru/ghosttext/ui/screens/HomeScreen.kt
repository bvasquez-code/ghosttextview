package com.cerru.ghosttext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cerru.ghosttext.ui.navigation.AppScreen

@Composable
fun HomeScreen(modifier: Modifier = Modifier, onNavigate: (AppScreen) -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "GhostText", fontWeight = FontWeight.Bold)
        Button(onClick = { onNavigate(AppScreen.CHANNELS) }, modifier = Modifier.fillMaxWidth()) {
            Text("Gestión de Canales")
        }
        Button(onClick = { onNavigate(AppScreen.CREATE_MESSAGE) }, modifier = Modifier.fillMaxWidth()) {
            Text("Crear mensaje (emisor)")
        }
        Button(onClick = { onNavigate(AppScreen.SEARCH_MESSAGE) }, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar mensaje (receptor)")
        }
        Button(onClick = { onNavigate(AppScreen.PASSWORDS) }, modifier = Modifier.fillMaxWidth()) {
            Text("Gestión de contraseñas")
        }
    }
}
