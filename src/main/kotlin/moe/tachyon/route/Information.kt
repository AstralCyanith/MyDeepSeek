package moe.tachyon.route

import moe.tachyon.utils.*
import moe.tachyon.route.utils.*
import io.github.smiley4.ktorswaggerui.dsl.routing.delete
import io.github.smiley4.ktorswaggerui.dsl.routing.get
import io.github.smiley4.ktorswaggerui.dsl.routing.post
import io.github.smiley4.ktorswaggerui.dsl.routing.route
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import moe.tachyon.dataClass.Permission
import moe.tachyon.dataClass.UserFull
import moe.tachyon.dataClass.UserId
import moe.tachyon.dataClass.UserId.Companion.toUserIdOrNull
import moe.tachyon.database.Users
import moe.tachyon.logger.MyDeepSeekLogger
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private val logger = MyDeepSeekLogger.getLogger()
fun Route.info() = route("", {
    tags = listOf("用户信息")
})
{
    get("/info/{id}", {
        description = "获取用户信息, id为0时获取当前登陆用户的信息"
        request {
            pathParameter<UserId>("id")
            {
                required = true
                description = "用户ID"
            }
        }
        response {
            statuses<UserFull>(
                HttpStatus.OK.copy(message = "获取用户信息成功"),
                bodyDescription = "当id为0, 即获取当前用户信息或user权限不低于ADMIN时返回",
                example = UserFull.example,
            )
            statuses(HttpStatus.NotFound, HttpStatus.NotLogin)
        }
    }) { getUserInfo() }

    post("/username", {
        description = "修改用户名"
        request {
            body<ChangeUsername>()
            {
                required = true
                description = "新用户名"
            }
        }
        response {
            statuses(HttpStatus.OK, HttpStatus.UsernameFormatError)
        }
    }) { changeUsername() }

    post("/avatar/{id}", {
        description = "修改头像, 修改他人头像要求user权限在ADMIN以上"
        request {
            pathParameter<UserId>("id")
            {
                required = true
                description = "要修改的用户ID, 0为当前登陆用户"
            }
            body<File>()
            {
                required = true
                mediaTypes(ContentType.Image.Any)
                description = "头像图片, 要求是正方形的"
            }
        }
        response {
            statuses(
                HttpStatus.OK,
                HttpStatus.NotFound,
                HttpStatus.Forbidden,
                HttpStatus.NotLogin,
                HttpStatus.PayloadTooLarge,
                HttpStatus.UnsupportedMediaType
            )
        }
    }) { changeAvatar() }

    get("/avatar/{id}", {
        description = "获取头像"
        request {
            pathParameter<UserId>("id")
            {
                required = true
                description = "要获取的用户ID, 0为当前登陆用户, 若id不为0则无需登陆, 否则需要登陆"
            }
        }
        response {
            statuses(ContentType.Image.PNG, HttpStatus.OK, bodyDescription = "获取到的头像, 总是png格式的")
            statuses(HttpStatus.BadRequest, HttpStatus.NotLogin)
        }
    }) { getAvatar() }

    delete("/avatar/{id}", {
        description = "删除头像, 即恢复默认头像, 删除他人头像要求user权限在ADMIN以上"
        request {
            pathParameter<UserId>("id")
            {
                required = true
                description = "要删除的用户ID, 0为当前登陆用户"
            }
        }
        response {
            statuses(
                HttpStatus.OK,
                HttpStatus.NotFound,
                HttpStatus.Forbidden,
                HttpStatus.NotLogin,
            )
        }
    }) { deleteAvatar() }
}

private suspend fun Context.getUserInfo()
{
    val id = call.parameters["id"]?.toUserIdOrNull() ?: finishCall(HttpStatus.BadRequest)
    val loginUser = getLoginUser()
    logger.config("user=${loginUser?.id} get user info id=$id")
    if (id == UserId(0))
    {
        if (loginUser == null) finishCall(HttpStatus.NotLogin)
        finishCall(HttpStatus.OK, loginUser)
    }
    else
    {
        val user = get<Users>().getUser(id) ?: finishCall(HttpStatus.NotFound)
        finishCall(HttpStatus.OK, user)
    }
}

private suspend fun Context.changeAvatar(): Nothing
{
    val id = call.parameters["id"]?.toUserIdOrNull() ?: finishCall(HttpStatus.BadRequest)
    val loginUser = getLoginUser() ?: finishCall(HttpStatus.NotLogin)
    // 检查body大小
    val size = call.request.headers["Content-Length"]?.toLongOrNull() ?: finishCall(HttpStatus.BadRequest)
    // 若图片大于10MB( 10 << 20 ), 返回请求实体过大
    if (size >= 10 shl 20) finishCall(HttpStatus.PayloadTooLarge)
    suspend fun getImage(): BufferedImage
    {
        return runCatching()
        {
            withContext(Dispatchers.IO)
            {
                ImageIO.read(call.receiveStream())
            }
        }.onFailure { logger.fine("接收头像失败", it) }.getOrNull() ?: finishCall(HttpStatus.UnsupportedMediaType)
    }
    logger.config("user=${loginUser} change avatar id=$id")
    if (id == UserId(0) && loginUser.permission >= Permission.NORMAL)
    {
        FileUtils.setAvatar(loginUser.id, getImage())
        finishCall(HttpStatus.OK)
    }
    else
    {
        if (loginUser.permission < Permission.ADMIN) finishCall(HttpStatus.Forbidden)
        val user = get<Users>().getUser(id) ?: finishCall(HttpStatus.NotFound)
        FileUtils.setAvatar(user.id, getImage())
        finishCall(HttpStatus.OK)
    }
}

private suspend fun Context.deleteAvatar()
{
    val id = call.parameters["id"]?.toUserIdOrNull() ?: finishCall(HttpStatus.BadRequest)
    val loginUser = getLoginUser() ?: finishCall(HttpStatus.NotLogin)
    if (id == UserId(0))
    {
        FileUtils.setDefaultAvatar(loginUser.id)
        finishCall(HttpStatus.OK)
    }
    else
    {
        if (loginUser.permission < Permission.ADMIN) finishCall(HttpStatus.Forbidden)
        val user = get<Users>().getUser(id) ?: finishCall(HttpStatus.NotFound)
        FileUtils.setDefaultAvatar(user.id)
        finishCall(HttpStatus.OK)
    }
}

private fun Context.getAvatar()
{
    val id = (call.parameters["id"]?.toUserIdOrNull() ?: finishCall(HttpStatus.BadRequest)).let {
        if (it == UserId(0)) getLoginUser()?.id ?: finishCall(HttpStatus.NotLogin)
        else it
    }
    val avatar = FileUtils.getAvatar(id)
    finishCallWithBytes(HttpStatus.OK, ContentType.Image.PNG) { ImageIO.write(avatar, "png", this) }
}

@Serializable
private data class ChangeUsername(val username: String)

private suspend fun Context.changeUsername()
{
    val loginUser = getLoginUser() ?: finishCall(HttpStatus.NotLogin)
    val username = call.receive<ChangeUsername>().username
    if (!checkUsername(username)) finishCall(HttpStatus.UsernameFormatError)
    get<Users>().setUsername(loginUser.id, username)
    finishCall(HttpStatus.OK)
}