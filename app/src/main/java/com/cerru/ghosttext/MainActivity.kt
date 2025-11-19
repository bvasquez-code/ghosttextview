package com.cerru.ghosttext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.cerru.ghosttext.model.dto.CreateMessageRequestDto
import com.cerru.ghosttext.service.GhostTextApi
import com.cerru.ghosttext.service.GhostTextService
import com.cerru.ghosttext.ui.theme.GhostTextViewTheme
import com.cerru.ghosttext.util.CryptoUtils
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost:7070/")
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient)
            .build()

        val api = retrofit.create(GhostTextApi::class.java)
        val service = GhostTextService(api)

        setContent {
            GhostTextViewTheme {
                val context = LocalContext.current
                val storage = remember { LocalStorage(context) }
                val channels = remember { mutableStateOf(storage.loadChannels()) }
                val passwords = remember { mutableStateOf(storage.loadPasswords()) }
                val screen = remember { mutableStateOf(AppScreen.HOME) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (screen.value) {
                        AppScreen.HOME -> HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigate = { screen.value = it }
                        )

                        AppScreen.CHANNELS -> ChannelManagementScreen(
                            modifier = Modifier.padding(innerPadding),
                            storage = storage,
                            service = service,
                            passwordsState = passwords,
                            channelsState = channels,
                            onBack = { screen.value = AppScreen.HOME }
                        )

                        AppScreen.CREATE_MESSAGE -> CreateMessageScreen(
                            modifier = Modifier.padding(innerPadding),
                            storage = storage,
                            service = service,
                            passwordsState = passwords,
                            channelsState = channels,
                            onBack = { screen.value = AppScreen.HOME }
                        )

                        AppScreen.SEARCH_MESSAGE -> SearchMessageScreen(
                            modifier = Modifier.padding(innerPadding),
                            storage = storage,
                            service = service,
                            passwordsState = passwords,
                            channelsState = channels,
                            onBack = { screen.value = AppScreen.HOME }
                        )

                        AppScreen.PASSWORDS -> PasswordManagerScreen(
                            modifier = Modifier.padding(innerPadding),
                            storage = storage,
                            passwordsState = passwords,
                            onBack = { screen.value = AppScreen.HOME }
                        )
                    }
                }
            }
        }
    }
}

enum class AppScreen { HOME, CHANNELS, CREATE_MESSAGE, SEARCH_MESSAGE, PASSWORDS }

@Composable
private fun HomeScreen(modifier: Modifier = Modifier, onNavigate: (AppScreen) -> Unit) {
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

@Composable
private fun ChannelManagementScreen(
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

@Composable
private fun PasswordSelector(passwords: List<PasswordEntry>, selectedPasswordId: MutableState<String?>) {
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

@Composable
private fun ChannelDisplay(code: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("channelCode: $code", fontWeight = FontWeight.Bold)
        TextButton(onClick = {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            manager.setPrimaryClip(ClipData.newPlainText("channelCode", code))
            Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
        }) {
            Text("Copiar")
        }
    }
}

@Composable
private fun CreateMessageScreen(
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

@Composable
private fun PasswordSuggestionField(passwordState: MutableState<String>, passwords: List<PasswordEntry>) {
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

@Composable
private fun SearchMessageScreen(
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

@Composable
private fun PasswordManagerScreen(
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
