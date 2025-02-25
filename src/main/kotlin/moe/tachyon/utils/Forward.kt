package moe.tachyon.utils

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import moe.tachyon.config.systemConfig
import moe.tachyon.dataClass.Message
import moe.tachyon.database.Users
import moe.tachyon.logger.MyDeepSeekLogger.getLogger
import moe.tachyon.plugin.contentNegotiation.contentNegotiationJson
import moe.tachyon.route.ChatData
import moe.tachyon.route.ChatParameter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class RequestBody(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean,
    val temperature: Double,
    val top_p: Double,
    val top_k: Int,
    val frequency_penalty: Double,
)

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
            header("Authorization", "Bearer " + systemConfig.apiKey)
            header("Content-Type", "application/json")
            setBody(
                RequestBody(
                    chatData.model.toString(),
                    chatData.message,
                    true,
                    chatParameter.temperature,
                    chatParameter.top_p,
                    chatParameter.top_k,
                    chatParameter.frequencyPenalty,
                )
            )
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