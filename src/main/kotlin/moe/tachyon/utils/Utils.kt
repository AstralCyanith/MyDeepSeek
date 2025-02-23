package moe.tachyon.utils

import moe.tachyon.logger.MyDeepSeekLogger
import moe.tachyon.plugin.contentNegotiation.contentNegotiationJson
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.util.*

private val logger = MyDeepSeekLogger.getLogger()

@Suppress("unused")
fun String?.toUUIDOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()
inline fun <reified T: Enum<T>> String?.toEnumOrNull(): T? =
    this?.runCatching { contentNegotiationJson.decodeFromString<T>(this) }?.getOrNull()

open class LineOutputStream(private val line: (String) -> Unit): OutputStream()
{
    private val arrayOutputStream = ByteArrayOutputStream()
    override fun write(b: Int)
    {
        if (b == '\n'.code)
        {
            val str: String
            synchronized(arrayOutputStream)
            {
                str = arrayOutputStream.toString()
                arrayOutputStream.reset()
            }
            runCatching { line(str) }
        }
        else
        {
            arrayOutputStream.write(b)
        }
    }
}

open class LinePrintStream(private val line: (String) -> Unit): PrintStream(LineOutputStream(line))
{
    override fun println(x: Any?) = x.toString().split('\n').forEach(line)

    override fun println() = println("" as Any?)
    override fun println(x: Boolean) = println(x as Any?)
    override fun println(x: Char) = println(x as Any?)
    override fun println(x: Int) = println(x as Any?)
    override fun println(x: Long) = println(x as Any?)
    override fun println(x: Float) = println(x as Any?)
    override fun println(x: Double) = println(x as Any?)
    override fun println(x: CharArray) = println(x.joinToString("") as Any?)
    override fun println(x: String?) = println(x as Any?)
}

/**
 * 检查密码是否合法
 * 要求密码长度在 8-20 之间，且仅包含数字、字母和特殊字符 !@#$%^&*()_+-=
 */
fun checkPassword(password: String): Boolean =
    password.length in 8..20 &&
            password.all { it.isLetterOrDigit() || it in "!@#$%^&*()_+-=" }

/**
 * 检查用户名是否合法
 * 要求用户名长度在 2-20 之间，且仅包含中文、数字、字母和特殊字符 _-.
 */
fun checkUsername(username: String): Boolean =
    username.length in 2..20 &&
            username.all { it in '\u4e00'..'\u9fa5' || it.isLetterOrDigit() || it in "_-." }

fun checkUserInfo(username: String, password: String): HttpStatus
{
    if (!checkPassword(password)) return HttpStatus.PasswordFormatError
    if (!checkUsername(username)) return HttpStatus.UsernameFormatError
    return HttpStatus.OK
}