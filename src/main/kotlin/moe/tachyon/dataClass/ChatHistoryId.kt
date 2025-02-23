package moe.tachyon.dataClass

import kotlinx.serialization.Serializable
import kotlin.let

@JvmInline
@Serializable
value class ChatHistoryId(val value: Long): Comparable<ChatHistoryId>
{
    override fun compareTo(other: ChatHistoryId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
    companion object
    {
        fun String.toChatHistoryId() = ChatHistoryId(toLong())
        fun String.toChatHistoryIdOrNull() = toLongOrNull()?.let(::ChatHistoryId)
        fun Number.toChatHistoryId() = ChatHistoryId(toLong())
    }
}