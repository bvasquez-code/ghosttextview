package com.cerru.ghosttext.model.dto

data class MessageResponseDto(
    val messageId: Long,
    val senderAlias: String?,
    val fakeText: String?,
    val realCipherTextBase64: String,
    val ivBase64: String,
    val authTagBase64: String?,
    val creationDate: String
)