package moe.tachyon.route

import io.github.smiley4.ktorswaggerui.dsl.routing.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import moe.tachyon.config.systemConfig
import moe.tachyon.dataClass.*
import moe.tachyon.dataClass.UserId.Companion.toUserIdOrNull
import moe.tachyon.database.ChatHistories
import moe.tachyon.route.utils.*
import moe.tachyon.utils.HttpStatus
import moe.tachyon.utils.statuses

fun Route.chat() = route("/chat",{
    tags = listOf("对话")
})
{
    post({
        description = "对话,模型参数仅管理员可设置"
        request {
            body<ChatData>()
            {
                required = true
                description = "对话信息"
                example("example", ChatData("message"))
            }
        }
        response {
            //TODO
            statuses(HttpStatus.Forbidden, HttpStatus.Unauthorized)
        }
    }) { chat() }

    get("/history",{
        description = "获得登录用户的历史对话记录,ROOT可获得所有用户"
        request {
            paged()
            queryParameter<UserId>("userId"){
                description = "ROOT权限时，获取的用户的ID"
                example(UserId(0))
            }
        }
        response {
            statuses<Slice<ChatHistory>>(HttpStatus.OK, example = sliceOf(ChatHistory.example))
            statuses(HttpStatus.Forbidden, HttpStatus.Unauthorized)
        }
    }) { getChatHistory() }

    post("/setGlobalModelConfig", {
        description = "设置全局默认模型参数,需要admin权限"
        request {
            body<ModelConfig>()
            {
                required = true
                description = "全局默认模型参数"
                example("example", ModelConfig.example)
            }
        }
        response {
            statuses(HttpStatus.OK)
            statuses(HttpStatus.Forbidden, HttpStatus.Unauthorized)
        }
    }) { setGlobalModelConfig() }

}

@Serializable
data class ChatData(
    val message: String,
    val modelConfig: ModelConfig? = null
)

private suspend fun Context.chat()
{
    val user = getLoginUser() ?: finishCall(HttpStatus.NotLogin)
    val chatData = call.receive<ChatData>()
    if(chatData.modelConfig != null && user.permission < Permission.ADMIN) finishCall(HttpStatus.Forbidden)
    //TODO
    finishCall(HttpStatus.OK)
}


private suspend fun Context.getChatHistory()
{
    val user = getLoginUser() ?: finishCall(HttpStatus.NotLogin)
    val id = call.parameters["userId"]?.let {
        if(user.permission != Permission.ROOT) finishCall(HttpStatus.Forbidden)
        it.toUserIdOrNull() ?: finishCall(HttpStatus.BadRequest)
    } ?: user.id
    val (begin, count) = call.getPage()
    val res = get<ChatHistories>().getChatHistory(id, begin, count)
    finishCall(HttpStatus.OK, res)
}

private suspend fun Context.setGlobalModelConfig()
{
    val user = getLoginUser() ?: finishCall(HttpStatus.NotLogin)
    if(user.permission < Permission.ADMIN) finishCall(HttpStatus.Forbidden)
    val config = call.receive<ModelConfig>()
    systemConfig = systemConfig.copy(defaultModel = config)
    finishCall(HttpStatus.OK)
}