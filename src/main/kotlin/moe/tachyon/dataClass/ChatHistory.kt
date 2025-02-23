package moe.tachyon.dataClass

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import moe.tachyon.plugin.contentNegotiation.contentNegotiationJson

@Serializable
data class ChatHistory(
    val id: ChatHistoryId,
    val user: UserId,
    val content: JsonElement,
    val time: Long,
)
{
    companion object
    {
        val example get() = ChatHistory(
            id = ChatHistoryId(1),
            user = UserId(1),
            content = contentNegotiationJson.parseToJsonElement("""
                []
                """.trimIndent()
            ),
            time = System.currentTimeMillis()
        )
    }
}