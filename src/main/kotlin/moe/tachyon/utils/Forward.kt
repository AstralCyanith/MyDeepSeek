package moe.tachyon.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import moe.tachyon.config.systemConfig
import moe.tachyon.database.Users
import moe.tachyon.logger.MyDeepSeekLogger.getLogger
import moe.tachyon.plugin.contentNegotiation.contentNegotiationJson
import moe.tachyon.route.ChatData
import moe.tachyon.route.ChatParameter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("MemberVisibilityCanBePrivate")
object Forward: KoinComponent
{
    val users: Users by inject()
    private val logger = getLogger()
    private val httpClient = HttpClient(Java)
    {
        engine()
        {
            pipelining = true
            dispatcher = Dispatchers.IO
            protocolVersion = java.net.http.HttpClient.Version.HTTP_2
        }
        install(ContentNegotiation)
        {
            json(contentNegotiationJson)
        }
    }

    suspend fun sendChat(chatData: ChatData): Flow<String> = flow {
        val chatParameter = chatData.chatParameter ?: ChatParameter(
            systemConfig.defaultModel.temperature,
            systemConfig.defaultModel.top_p,
            systemConfig.defaultModel.top_k,
            systemConfig.defaultModel.frequencyPenalty,
        )
        val response = httpClient.post(systemConfig.apiUrl,) {
            headers {
                append("Authorization", "Bearer " + systemConfig.apiKey)
                append("Content-Type", "application/json")
            }
            setBody("""{
                "model": ${chatData.model}.,
                "messages": ${Json.encodeToString(chatData.message)},
                "stream": True,
                "temperature": ${chatParameter.temperature},
                "top_p": ${chatParameter.top_p},
                "top_k": ${chatParameter.top_k},
                "frequency_penalty": ${chatParameter.frequencyPenalty},
            }""".trimIndent())
        }
        response.bodyAsChannel().toInputStream().use { stream ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                val chunk = String(buffer, 0, bytesRead)
                emit(chunk)
            }
        }

    }

}