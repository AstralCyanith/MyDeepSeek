package moe.tachyon.route

import io.github.smiley4.ktorswaggerui.dsl.routing.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import moe.tachyon.dataClass.Permission
import moe.tachyon.database.Users
import moe.tachyon.utils.*
import moe.tachyon.JWTAuth
import moe.tachyon.dataClass.UserId
import moe.tachyon.route.utils.*

fun Route.basic() = route("/auth",{
    tags = listOf("账户")
}) {
    post("/register", {
        description = "注册, 若成功返回token, 需要ROOT权限"
        request {
            body<RegisterInfo>
            {
                required = true
                description = "注册信息, 权限不传默认为NORMAL"
                example("example", RegisterInfo("username", "password", Permission.NORMAL))
            }
        }
        this.response {
            statuses<JWTAuth.Token>(HttpStatus.OK, example = JWTAuth.Token("token"))
            statuses(
                HttpStatus.NotLogin,
                HttpStatus.Forbidden,
                HttpStatus.UsernameExist,
                HttpStatus.UsernameFormatError,
                HttpStatus.PasswordFormatError.subStatus(code = 1),
                HttpStatus.NotInWhitelist.subStatus(code = 2)
            )
        }
    }) { register() }

    post("/login", {
        description = "登陆, 若成功返回token"
        request {
            body<Login>()
            {
                required = true
                description = "登陆信息, id(用户ID)和username(用户昵称)二选一"
                example("example", Login(username = "NullAqua", password = "password", id = UserId(0)))
            }
        }
        this.response {
            statuses<JWTAuth.Token>(HttpStatus.OK, example = JWTAuth.Token("token"))
            statuses(
                HttpStatus.PasswordError,
                HttpStatus.AccountNotExist,
            )
        }
    }) { login() }

    post("/changePassword", {
        description = "修改密码"
        request {
            body<ChangePasswordInfo>
            {
                required = true
                description = "修改密码信息,ROOT用户修改不需要旧密码"
                example("example", ChangePasswordInfo("oldPassword", "newPassword"))
            }
        }
        this.response {
            statuses<JWTAuth.Token>(HttpStatus.OK, example = JWTAuth.Token("token"))
            statuses(
                HttpStatus.NotLogin,
                HttpStatus.PasswordError.subStatus(code = 1),
                HttpStatus.PasswordFormatError,
            )
        }
    }) { changePassword() }
}

@Serializable
private data class RegisterInfo(
    val username: String,
    val password: String,
    val permission: Permission = Permission.NORMAL
)
private val registerLocks = Locks<String>()

private suspend fun Context.register()
{
    val user = getLoginUser() ?: finishCall(HttpStatus.NotLogin)
    if(user.permission < Permission.ROOT) finishCall(HttpStatus.Forbidden)
    val registerInfo: RegisterInfo = call.receive()

    //检查用户名是否合法
    checkUserInfo(registerInfo.username, registerInfo.password).apply {
        if (this != HttpStatus.OK) finishCall(this)
    }

    val id = registerLocks.withLock(registerInfo.username)
    {
        // 创建用户
        if (get<Users>().getUserByUsername(registerInfo.username) != null) finishCall(HttpStatus.UsernameExist)
        val id = get<Users>().createUser(
            username = registerInfo.username,
            password = registerInfo.password,
            permission = registerInfo.permission
        )
        id
    }

    // 创建成功, 返回token
    val token = JWTAuth.makeToken(id)
    finishCall(HttpStatus.OK, token)
}

@Serializable
private data class Login(val username: String? = null, val id: UserId? = null, val password: String)

private suspend fun Context.login()
{
    val users = get<Users>()
    val loginInfo: Login = call.receive()
    val checked =
        if (loginInfo.id != null) loginInfo.id.takeIf { users.checkLogin(loginInfo.id, loginInfo.password) }
        else if (loginInfo.username != null) users.checkLogin(loginInfo.username, loginInfo.password)
        else finishCall(HttpStatus.BadRequest)
    // 若登陆失败，返回错误信息
    if (checked == null) finishCall(HttpStatus.PasswordError)
    if (users.getUser(checked)?.permission == Permission.BANNED) finishCall(HttpStatus.Prohibit)
    val token = JWTAuth.makeToken(checked)
    finishCall(HttpStatus.OK, token)
}

@Serializable
private data class ChangePasswordInfo(val oldPassword: String?, val newPassword: String)

private suspend fun Context.changePassword()
{
    val users = get<Users>()

    val (oldPassword, newPassword) = call.receive<ChangePasswordInfo>()
    val user = getLoginUser() ?: finishCall(HttpStatus.NotLogin)
    if (oldPassword == null && user.permission < Permission.ROOT) finishCall(HttpStatus.PasswordError)
    if (oldPassword != null && !users.checkLogin(user.id, oldPassword)) finishCall(HttpStatus.PasswordError)
    if (!checkPassword(newPassword)) finishCall(HttpStatus.PasswordFormatError)
    users.setPassword(user.id, newPassword)
    val token = JWTAuth.makeToken(user.id)
    finishCall(HttpStatus.OK, token)
}