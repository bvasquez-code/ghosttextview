package com.cerru.ghosttext.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.cerru.ghosttext.data.PasswordEntry
import com.cerru.ghosttext.model.dto.CreateMessageRequestDto
import com.cerru.ghosttext.service.GhostTextService
import com.cerru.ghosttext.ui.components.PasswordSuggestionField
import com.cerru.ghosttext.util.CryptoUtils
import kotlinx.coroutines.launch

@Composable
fun CreateMessageScreen(
    modifier: Modifier = Modifier,
    service: GhostTextService,
    passwordsState: MutableState<List<PasswordEntry>>,
    channelsState: MutableState<List<ChannelEntry>>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val selectedChannel = remember { mutableStateOf<String?>(channelsState.value.firstOrNull()?.channelCode) }
    val fakeText = remember { mutableStateOf("") }
    val realText = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val senderAlias = remember { mutableStateOf("") }
    val resultMessage = remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Crear mensaje", fontWeight = FontWeight.Bold)
        }

        Text("Selecciona canal")
        channelsState.value.forEach { entry ->
            OutlinedButton(
                onClick = { selectedChannel.value = entry.channelCode },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(entry.channelCode)
            }
        }

        OutlinedTextField(
            value = senderAlias.value,
            onValueChange = { senderAlias.value = it },
            label = { Text("Alias del emisor (opcional)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fakeText.value,
            onValueChange = { fakeText.value = it },
            label = { Text("fakeText") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = realText.value,
            onValueChange = { realText.value = it },
            label = { Text("Texto real") },
            modifier = Modifier.fillMaxWidth()
        )
        PasswordSuggestionField(passwordState = password, passwords = passwordsState.value)

        Button(onClick = {
            scope.launch {
                val channel = channelsState.value.firstOrNull { it.channelCode == selectedChannel.value }
                if (channel == null) {
                    Toast.makeText(context, "Selecciona un canal", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                try {
                    val payload = CryptoUtils.encrypt(realText.value, password.value, channel.saltBase64)
                    val request = CreateMessageRequestDto(
                        senderAlias = senderAlias.value.ifBlank { null },
                        fakeText = fakeText.value,
                        realCipherTextBase64 = payload.cipherTextBase64,
                        ivBase64 = payload.ivBase64,
                        authTagBase64 = payload.authTagBase64
                    )
                    val response = service.sendMessage(channel.channelCode, request)
                    resultMessage.value = "Mensaje enviado con id ${response.messageId}"
                } catch (ex: Exception) {
                    resultMessage.value = "Error: ${ex.message}"
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Enviar mensaje")
        }
        resultMessage.value?.let { Text(it) }
        if (fakeText.value.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("FakeText listo para copiar")
                TextButton(onClick = {
                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    manager.setPrimaryClip(ClipData.newPlainText("fakeText", fakeText.value))
                    Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                }) { Text("Copiar") }
            }
        }
    }
}
