package com.cerru.ghosttext.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cerru.ghosttext.data.LocalStorage
import com.cerru.ghosttext.service.GhostTextService
import com.cerru.ghosttext.ui.screens.ChannelManagementScreen
import com.cerru.ghosttext.ui.screens.CreateMessageScreen
import com.cerru.ghosttext.ui.screens.HomeScreen
import com.cerru.ghosttext.ui.screens.PasswordManagerScreen
import com.cerru.ghosttext.ui.screens.SearchMessageScreen

enum class AppScreen { HOME, CHANNELS, CREATE_MESSAGE, SEARCH_MESSAGE, PASSWORDS }

@Composable
fun GhostTextApp(
    storage: LocalStorage,
    service: GhostTextService
) {
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
