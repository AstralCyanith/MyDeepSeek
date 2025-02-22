@file:Suppress("NOTHING_TO_INLINE", "unused")

package moe.tachyon.route.utils

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import moe.tachyon.utils.HttpStatus
import moe.tachyon.utils.respond
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class CallFinish(val block: suspend ApplicationCall.() -> Unit): RuntimeException()
{
    companion object
}

inline fun finishCall(httpStatus: HttpStatus): Nothing =
    throw CallFinish { respond(httpStatus) }
inline fun <reified T: Any>finishCall(httpStatus: HttpStatus, body: T): Nothing =
    throw CallFinish { respond<T>(httpStatus, body) }
inline fun finishCallWithText(httpStatus: HttpStatus, text: String, contentType: ContentType? = null): Nothing =
    throw CallFinish { respondText(text, contentType, httpStatus.code) }
inline fun finishCallWithBytes(httpStatus: HttpStatus, contentType: ContentType, bytes: ByteArray): Nothing =
    throw CallFinish { respondBytes(bytes, contentType, httpStatus.code) }
inline fun finishCallWithBytes(httpStatus: HttpStatus, contentType: ContentType, inputStream: InputStream): Nothing =
    throw CallFinish { respondOutputStream(contentType, httpStatus.code) { inputStream.copyTo(this) } }
inline fun finishCallWithBytes(httpStatus: HttpStatus, contentType: ContentType, noinline producer: suspend OutputStream.() -> Unit): Nothing =
    throw CallFinish { respondOutputStream(contentType, httpStatus.code, producer) }
inline fun finishCallWithFile(httpStatus: HttpStatus, contentType: ContentType, file: File): Nothing =
    throw CallFinish { respondOutputStream(contentType, httpStatus.code, file.length()) { file.inputStream().copyTo(this) } }
inline fun finishCallWithRedirect(location: String): Nothing =
    throw CallFinish { respondRedirect(location) }