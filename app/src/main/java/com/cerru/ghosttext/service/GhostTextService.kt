package com.cerru.ghosttext.service

import com.cerru.ghosttext.model.dto.ChannelInfoDto
import com.cerru.ghosttext.model.dto.CreateChannelResponseDto
import com.cerru.ghosttext.model.dto.CreateMessageRequestDto
import com.cerru.ghosttext.model.dto.MessageResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GhostTextService(
    private val api: GhostTextApi
) {

    /**
     * Crea un canal en el backend.
     */
    suspend fun createChannel(): CreateChannelResponseDto = withContext(Dispatchers.IO) {
        api.createChannel()
    }

    /**
     * Obtiene información de un canal (incluye el salt en Base64).
     */
    suspend fun getChannelInfo(channelCode: String): ChannelInfoDto = withContext(Dispatchers.IO) {
        api.getChannelInfo(channelCode)
    }

    /**
     * Envía un mensaje al backend.
     * Tú armas el CreateMessageRequestDto fuera (con el cifrado ya hecho).
     */
    suspend fun sendMessage(
        channelCode: String,
        request: CreateMessageRequestDto
    ): MessageResponseDto = withContext(Dispatchers.IO) {
        api.createMessage(channelCode, request)
    }

    /**
     * Lista todos los mensajes de un canal.
     */
    suspend fun listMessages(channelCode: String): List<MessageResponseDto> = withContext(Dispatchers.IO) {
        api.listMessages(channelCode)
    }

    /**
     * Busca el último mensaje por fakeText en un canal.
     */
    suspend fun findLastMessageByFakeText(
        channelCode: String,
        fakeText: String
    ): MessageResponseDto = withContext(Dispatchers.IO) {
        api.findLastMessageByFakeText(channelCode, fakeText)
    }
}