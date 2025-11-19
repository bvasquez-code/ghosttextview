package com.cerru.ghosttext.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cerru.ghosttext.data.PasswordEntry

@Composable
fun PasswordSelector(passwords: List<PasswordEntry>, selectedPasswordId: MutableState<String?>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Selecciona contraseña guardada")
        passwords.forEach { entry ->
            val label = entry.alias?.takeIf { it.isNotBlank() } ?: entry.id.take(6)
            OutlinedButton(
                onClick = { selectedPasswordId.value = entry.id },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(label)
            }
        }
    }
}

@Composable
fun PasswordSuggestionField(passwordState: MutableState<String>, passwords: List<PasswordEntry>) {
    val suggestions = passwords.mapNotNull { entry ->
        val label = entry.alias?.takeIf { it.isNotBlank() } ?: entry.id.take(6)
        label to entry.password
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = passwordState.value,
            onValueChange = { passwordState.value = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth()
        )
        if (suggestions.isNotEmpty()) {
            Text("Sugerencias")
            suggestions.forEach { (label, pwd) ->
                TextButton(onClick = { passwordState.value = pwd }) { Text(label) }
            }
        }
    }
}
