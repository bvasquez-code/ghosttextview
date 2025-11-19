package com.cerru.ghosttext.service

import com.cerru.ghosttext.model.dto.ChannelInfoDto
import com.cerru.ghosttext.model.dto.CreateChannelResponseDto
import com.cerru.ghosttext.model.dto.CreateMessageRequestDto
import com.cerru.ghosttext.model.dto.MessageResponseDto

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GhostTextApi {

    @POST("api/channels")
    suspend fun createChannel(): CreateChannelResponseDto

    @GET("api/channels/{channelCode}")
    suspend fun getChannelInfo(
        @Path("channelCode") channelCode: String
    ): ChannelInfoDto

    @POST("api/channels/{channelCode}/messages")
    suspend fun createMessage(
        @Path("channelCode") channelCode: String,
        @Body request: CreateMessageRequestDto
    ): MessageResponseDto

    @GET("api/channels/{channelCode}/messages")
    suspend fun listMessages(
        @Path("channelCode") channelCode: String
    ): List<MessageResponseDto>

    @GET("api/channels/{channelCode}/messages/search")
    suspend fun findLastMessageByFakeText(
        @Path("channelCode") channelCode: String,
        @Query("fakeText") fakeText: String
    ): MessageResponseDto
}