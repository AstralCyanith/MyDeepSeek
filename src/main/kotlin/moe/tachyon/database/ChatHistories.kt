package moe.tachyon.database

import kotlinx.serialization.json.JsonElement
import moe.tachyon.dataClass.ChatHistory
import moe.tachyon.dataClass.ChatHistoryId
import moe.tachyon.dataClass.Message
import moe.tachyon.dataClass.Slice
import moe.tachyon.dataClass.UserId
import moe.tachyon.database.utils.*
import moe.tachyon.plugin.contentNegotiation.dataJson
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.selectAll

class ChatHistories: SqlDao<ChatHistories.ChatHistoryTable>(ChatHistoryTable)
{
    /**
     * 聊天记录表
     */
    object ChatHistoryTable: IdTable<ChatHistoryId>("chat_history")
    {
        override val id = chatHistoryId("id").autoIncrement().entityId()
        val user = reference("user", Users.UserTable).index()
        val content = jsonb<List<Message>>("content", dataJson)
        val time = timestampWithTimeZone("time").defaultExpression(CurrentTimestampWithTimeZone)
        override val primaryKey = PrimaryKey(id)
    }

    private fun deserialize(row: ResultRow) = ChatHistory(
        id = row[ChatHistoryTable.id].value,
        user = row[ChatHistoryTable.user].value,
        content = row[ChatHistoryTable.content],
        time = row[ChatHistoryTable.time].toInstant().toEpochMilli(),
    )

    suspend fun insertChatHistory( user: UserId, content: List<Message>): ChatHistoryId = query()
    {
        insertAndGetId {
            it[ChatHistoryTable.user] = user
            it[ChatHistoryTable.content] = content
        }.value
    }

    /**
     * 获取历史对话数据, 按时间倒序排列, 即最新的版本在前.
     * @param user 用户ID
     * @return 返回历史对话数据切片
     */
    suspend fun getChatHistory(user: UserId, begin: Long, count: Int): Slice<ChatHistory> = query()
    {
        selectAll()
            .where{ ChatHistoryTable.user eq user }
            .orderBy(time to SortOrder.DESC)
            .asSlice(begin, count)
            .map(::deserialize)
    }
}