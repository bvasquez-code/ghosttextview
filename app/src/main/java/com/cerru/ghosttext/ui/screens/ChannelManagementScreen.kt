package com.cerru.ghosttext.ui.screens

import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cerru.ghosttext.data.ChannelEntry
import com.cerru.ghosttext.data.LocalStorage
import com.cerru.ghosttext.data.PasswordEntry
import com.cerru.ghosttext.service.GhostTextService
import com.cerru.ghosttext.ui.components.ChannelDisplay
import com.cerru.ghosttext.ui.components.PasswordSelector
import kotlinx.coroutines.launch

@Composable
fun ChannelManagementScreen(
    modifier: Modifier = Modifier,
    storage: LocalStorage,
    service: GhostTextService,
    passwordsState: MutableState<List<PasswordEntry>>,
    channelsState: MutableState<List<ChannelEntry>>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val selectedPasswordId = remember { mutableStateOf<String?>(null) }
    val newPassword = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val alias = remember { mutableStateOf("") }
    val warningAccepted = remember { mutableStateOf(false) }
    val createChannelResult = remember { mutableStateOf<String?>(null) }
    val manualChannelCode = remember { mutableStateOf("") }
    val infoMessage = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        selectedPasswordId.value = passwordsState.value.firstOrNull()?.id
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Gestión de canales", fontWeight = FontWeight.Bold)
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Crear canal nuevo", fontWeight = FontWeight.Bold)
                PasswordSelector(passwordsState.value, selectedPasswordId)

                OutlinedTextField(
                    value = newPassword.value,
                    onValueChange = { newPassword.value = it },
                    label = { Text("Nueva contraseña") },
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
                    label = { Text("Alias (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "La contraseña es secreta. No viaja al servidor y no se puede recuperar si la olvidas. Debes recordarla siempre.",
                    fontWeight = FontWeight.SemiBold
                )
                Button(onClick = {
                    if (!warningAccepted.value) {
                        Toast.makeText(
                            context,
                            "La contraseña es secreta. No viaja al servidor y no se puede recuperar si la olvidas. Debes recordarla siempre.",
                            Toast.LENGTH_LONG
                        ).show()
                        warningAccepted.value = true
                    }
                    scope.launch {
                        val passwordId = ensurePasswordSelected(
                            storage,
                            passwordsState,
                            selectedPasswordId,
                            newPassword.value,
                            confirmPassword.value,
                            alias.value,
                            context
                        )
                        if (passwordId == null) return@launch
                        try {
                            val response = service.createChannel()
                            val updated = storage.addChannel(
                                ChannelEntry(
                                    channelCode = response.channelCode,
                                    saltBase64 = response.saltBase64,
                                    passwordId = passwordId
                                )
                            )
                            channelsState.value = updated
                            createChannelResult.value = response.channelCode
                        } catch (ex: Exception) {
                            infoMessage.value = "Error al crear canal: ${ex.message}"
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Crear canal")
                }
                createChannelResult.value?.let { code ->
                    ChannelDisplay(code)
                }
                infoMessage.value?.let { Text(it) }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Registrar canal manualmente", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = manualChannelCode.value,
                    onValueChange = { manualChannelCode.value = it },
                    label = { Text("channelCode") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    scope.launch {
                        try {
                            val info = service.getChannelInfo(manualChannelCode.value)
                            if (info.status == "A") {
                                val updated = storage.addChannel(
                                    ChannelEntry(
                                        channelCode = info.channelCode,
                                        saltBase64 = info.saltBase64,
                                        passwordId = null
                                    )
                                )
                                channelsState.value = updated
                                infoMessage.value = "Canal registrado"
                            } else {
                                infoMessage.value = "El canal no está activo"
                            }
                        } catch (ex: Exception) {
                            infoMessage.value = "Error: ${ex.message}"
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Registrar")
                }
            }
        }
    }
}

private fun ensurePasswordSelected(
    storage: LocalStorage,
    passwordsState: MutableState<List<PasswordEntry>>,
    selectedPasswordId: MutableState<String?>,
    newPassword: String,
    confirmPassword: String,
    alias: String,
    context: Context
): String? {
    if (newPassword.isNotBlank() || confirmPassword.isNotBlank()) {
        if (newPassword != confirmPassword) {
            Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return null
        }
        val entry = PasswordEntry(alias = alias.ifBlank { null }, password = newPassword)
        passwordsState.value = storage.addPassword(entry)
        selectedPasswordId.value = entry.id
    }
    val chosen = selectedPasswordId.value
    if (chosen == null) {
        Toast.makeText(context, "Selecciona o crea una contraseña", Toast.LENGTH_SHORT).show()
        return null
    }
    return chosen
}
