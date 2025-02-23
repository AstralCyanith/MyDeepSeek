package moe.tachyon.dataClass

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import moe.tachyon.plugin.contentNegotiation.contentNegotiationJson

enum class Role
{
    USER,
    ASSISTANT,
    SYSTEM,
}

@Serializable
data class Message(
    val role: Role,
    val content: String,
)

@Serializable
data class ChatHistory(
    val id: ChatHistoryId,
    val user: UserId,
    val content: List<Message>,
    val time: Long,
)
{
    companion object
    {
        val example get() = ChatHistory(
            id = ChatHistoryId(1),
            user = UserId(1),
            content = listOf(
                Message(Role.SYSTEM, "You are a AI assistant"),
                Message(Role.USER, "Hello"),
                Message(Role.ASSISTANT, "Hi"),
            ),
            time = System.currentTimeMillis()
        )
    }
}