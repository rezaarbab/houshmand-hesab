package com.houshmandhesab.app.ai

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

data class ChatMessage(
    val role: String,
    val content: String
)

data class AiRunRequest(
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 1024,
    val temperature: Double = 0.6
)

data class AiRunResult(val response: String? = null)

data class AiRunResponse(
    val result: AiRunResult? = null,
    val success: Boolean = false,
    val errors: List<Any?>? = null,
    val messages: List<Any?>? = null
)

interface CloudflareApi {
    @POST("accounts/{accountId}/ai/run/{model}")
    suspend fun run(
        @Path("accountId") accountId: String,
        @Path(value = "model", encoded = true) model: String,
        @Header("Authorization") auth: String,
        @Body body: AiRunRequest
    ): AiRunResponse
}
