@file:Suppress("PackageDirectoryMismatch")

package moe.tachyon.plugin.authentication

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import moe.tachyon.JWTAuth
import moe.tachyon.JWTAuth.initJwtAuth
import moe.tachyon.config.apiDocsConfig
import moe.tachyon.utils.HttpStatus
import moe.tachyon.utils.respond

/**
 * 安装登陆验证服务
 */
fun Application.installAuthentication() = install(Authentication)
{
    this@installAuthentication.initJwtAuth()

    // 此登陆仅用于api文档的访问, 见ApiDocs插件
    basic("auth-api-docs")
    {
        realm = "Access to the Swagger UI"
        validate()
        {
            if (it.name == apiDocsConfig.name && it.password == apiDocsConfig.password)
                UserIdPrincipal(it.name)
            else null
        }
    }

    jwt("auth")
    {
        verifier(JWTAuth.makeJwtVerifier()) // 设置验证器
        validate() // 设置验证函数
        {
            runCatching { JWTAuth.checkToken(it.payload) }.getOrNull()
        }
        challenge { _, _ -> call.respond(HttpStatus.InvalidToken) }
    }
}