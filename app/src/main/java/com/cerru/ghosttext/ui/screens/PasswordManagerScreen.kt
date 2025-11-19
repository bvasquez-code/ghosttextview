package com.cerru.ghosttext.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cerru.ghosttext.data.LocalStorage
import com.cerru.ghosttext.data.PasswordEntry

@Composable
fun PasswordManagerScreen(
    modifier: Modifier = Modifier,
    storage: LocalStorage,
    passwordsState: MutableState<List<PasswordEntry>>,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val newPassword = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val alias = remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Contraseñas locales", fontWeight = FontWeight.Bold)
        }

        passwordsState.value.forEach { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(entry.alias ?: "Sin alias")
                        Text("ID: ${entry.id.take(6)}...")
                    }
                    TextButton(onClick = {
                        passwordsState.value = storage.deletePassword(entry.id)
                    }) { Text("Eliminar") }
                }
            }
        }

        Text("Agregar contraseña nueva", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = newPassword.value,
            onValueChange = { newPassword.value = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPassword.value,
            onValueChange = { confirmPassword.value = it },
            label = { Text("Confirmar contraseña") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = alias.value,
            onValueChange = { alias.value = it },
            label = { Text("Alias opcional") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            if (newPassword.value != confirmPassword.value) {
                Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@Button
            }
            val entry = PasswordEntry(alias = alias.value.ifBlank { null }, password = newPassword.value)
            passwordsState.value = storage.addPassword(entry)
            newPassword.value = ""
            confirmPassword.value = ""
            alias.value = ""
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar contraseña")
        }
    }
}
