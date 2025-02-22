@file:Suppress("PackageDirectoryMismatch")

package moe.tachyon.plugin.authentication

import moe.tachyon.config.apiDocsConfig
import moe.tachyon.logger.SubQuizLogger
import moe.tachyon.route.utils.finishCall
import moe.tachyon.utils.HttpStatus
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*

/**
 * 安装登陆验证服务
 */
fun Application.installAuthentication() = install(Authentication)
{
    val logger = SubQuizLogger.getLogger()
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

    bearer("auth")
    {
        authHeader()
        {
            TODO("这部分是call中获取token的方法，也可以不设置[authHeader]，默认行为是从header中获取Authorization字段")
        }
        authenticate()
        {
            TODO("这部分是验证token，this是call，it是解析后的token。你需要在这里验证token的有效性，")
            TODO("若有效，则返回一个登录的用户的对象")
        }
    }
}