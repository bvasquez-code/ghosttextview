package com.cerru.ghosttext.model.dto

data class CreateMessageRequestDto(
    val senderAlias: String? = null,
    val fakeText: String? = null,
    val realCipherTextBase64: String,
    val ivBase64: String,
    val authTagBase64: String? = null
)
