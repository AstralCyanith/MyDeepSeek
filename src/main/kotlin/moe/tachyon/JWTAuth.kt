package moe.tachyon

import at.favre.lib.crypto.bcrypt.BCrypt
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.util.pipeline.*
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.Serializable
import moe.tachyon.console.SimpleAnsiColor.Companion.CYAN
import moe.tachyon.console.SimpleAnsiColor.Companion.RED
import moe.tachyon.dataClass.UserFull
import moe.tachyon.dataClass.UserId
import moe.tachyon.dataClass.UserId.Companion.toUserId
import moe.tachyon.database.Users
import moe.tachyon.logger.MyDeepSeekLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.OffsetDateTime
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * JWT验证
 */
@Suppress("MemberVisibilityCanBePrivate")
object JWTAuth: KoinComponent
{
    private val logger = MyDeepSeekLogger.getLogger()
    private val users: Users by inject()

    @Serializable
    data class Token(val token: String)

    /**
     * JWT密钥
     */
    private lateinit var SECRET_KEY: String

    /**
     * JWT算法
     */
    private lateinit var algorithm: Algorithm

    /**
     * 用户JWT Token有效期
     */
    val USER_TOKEN_VALIDITY: Duration = 90.days

    fun Application.initJwtAuth()
    {
        // 从配置文件中读取密钥
        val key = environment.config.propertyOrNull("jwt.secret")?.getString()
        if (key == null)
        {
            logger.info("${CYAN}jwt.secret${RED} not found in config file, use random secret key")
            SECRET_KEY = UUID.randomUUID().toString()
        }
        else
        {
            SECRET_KEY = key
        }
        // 初始化JWT算法
        algorithm = Algorithm.HMAC512(SECRET_KEY)
    }

    /**
     * 生成验证器
     */
    fun makeJwtVerifier(): JWTVerifier = JWT.require(algorithm).build()

    fun makeToken(user: UserId): Token = JWT.create()
        .withSubject("Authentication")
        .withClaim("id", user.value)
        .withExpiresAt((OffsetDateTime.now().toInstant().toKotlinInstant() + USER_TOKEN_VALIDITY).toJavaInstant())
        .withIssuer("subit")
        .withIssuedAt(OffsetDateTime.now().toInstant().toKotlinInstant().toJavaInstant())
        .sign(algorithm)
        .let(::Token)

    suspend fun checkToken(token: Payload): UserFull?
    {
        val (userInfo, lastPswChange) =
            users.getUserWithLastPasswordChange(token.getClaim("id").asInt().toUserId()) ?: return null
        val tokenTime = token.issuedAtAsInstant.toKotlinInstant()
        if (lastPswChange > tokenTime)
        {
            logger.config("User token issued before password change, last change time: $lastPswChange, token time: $tokenTime")
            return null
        }
        return userInfo
    }

    fun PipelineContext<*, ApplicationCall>.getLoginUser(): UserFull? = call.principal<UserFull>()

    private val hasher = BCrypt.with(BCrypt.Version.VERSION_2B)
    private val verifier = BCrypt.verifyer(BCrypt.Version.VERSION_2B)

    /**
     * 在数据库中保存密码的加密
     */
    fun encryptPassword(password: String): String = hasher.hashToString(12, password.toCharArray())
    fun verifyPassword(password: String, hash: String): Boolean = verifier.verify(password.toCharArray(), hash).verified
}
