package com.cerru.ghosttext.ui.screens

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cerru.ghosttext.data.ChannelEntry
import com.cerru.ghosttext.data.LocalStorage
import com.cerru.ghosttext.data.PasswordEntry
import com.cerru.ghosttext.service.GhostTextService
import com.cerru.ghosttext.ui.components.PasswordSuggestionField
import com.cerru.ghosttext.util.CryptoUtils
import kotlinx.coroutines.launch

@Composable
fun SearchMessageScreen(
    modifier: Modifier = Modifier,
    storage: LocalStorage,
    service: GhostTextService,
    passwordsState: MutableState<List<PasswordEntry>>,
    channelsState: MutableState<List<ChannelEntry>>,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val channelCode = remember { mutableStateOf("") }
    val fakeText = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val decrypted = remember { mutableStateOf<String?>(null) }
    val messageInfo = remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Atrás") }
            Text("Buscar y descifrar", fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = channelCode.value,
            onValueChange = { channelCode.value = it },
            label = { Text("channelCode") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = fakeText.value,
            onValueChange = { fakeText.value = it },
            label = { Text("fakeText") },
            modifier = Modifier.fillMaxWidth()
        )
        PasswordSuggestionField(passwordState = password, passwords = passwordsState.value)

        Button(onClick = {
            scope.launch {
                try {
                    var channel = storage.findChannel(channelCode.value)
                    if (channel == null) {
                        val info = service.getChannelInfo(channelCode.value)
                        channel = ChannelEntry(
                            channelCode = info.channelCode,
                            saltBase64 = info.saltBase64,
                            passwordId = null
                        )
                        channelsState.value = storage.addChannel(channel)
                    }
                    val message = service.findLastMessageByFakeText(channel.channelCode, fakeText.value)
                    val text = CryptoUtils.decrypt(
                        cipherTextBase64 = message.realCipherTextBase64,
                        ivBase64 = message.ivBase64,
                        authTagBase64 = message.authTagBase64 ?: "",
                        password = password.value,
                        saltBase64 = channel.saltBase64
                    )
                    decrypted.value = text
                    messageInfo.value = "Mensaje ${message.messageId} de ${message.senderAlias ?: "desconocido"}"
                } catch (ex: Exception) {
                    decrypted.value = null
                    messageInfo.value = "Error: ${ex.message}"
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar y descifrar")
        }
        messageInfo.value?.let { Text(it) }
        decrypted.value?.let { Text("Texto real: $it", fontWeight = FontWeight.Bold) }
    }
}
