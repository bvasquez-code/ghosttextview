package com.cerru.ghosttext.model.dto

data class CreateChannelResponseDto(
    val channelCode: String,
    val saltBase64: String
)