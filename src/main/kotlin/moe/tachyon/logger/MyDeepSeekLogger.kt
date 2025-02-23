package moe.tachyon.logger

import moe.tachyon.config.loggerConfig
import moe.tachyon.console.AnsiStyle.Companion.RESET
import moe.tachyon.console.Console
import moe.tachyon.console.SimpleAnsiColor
import moe.tachyon.console.SimpleAnsiColor.Companion.CYAN
import moe.tachyon.console.SimpleAnsiColor.Companion.PURPLE
import moe.tachyon.logger.MyDeepSeekLogger.safe
import moe.tachyon.workDir
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import me.nullaqua.api.reflect.CallerSensitive
import me.nullaqua.api.kotlin.reflect.KallerSensitive
import me.nullaqua.api.kotlin.reflect.getCallerClass
import me.nullaqua.api.kotlin.reflect.getCallerClasses
import me.nullaqua.api.kotlin.utils.LoggerUtil
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.*
import java.util.logging.Formatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.jvm.optionals.getOrDefault
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName
import kotlin.time.Duration

/**
 * logger系统
 */
@Suppress("MemberVisibilityCanBePrivate")
object MyDeepSeekLogger
{
    val globalLogger = LoggerUtil(Logger.getLogger(""))
    fun getLogger(name: String): LoggerUtil = LoggerUtil(Logger.getLogger(name))
    fun getLogger(clazz: Class<*>): LoggerUtil
    {
        var c = clazz
        while (c.declaringClass != null) c = c.declaringClass
        val name = c.kotlin.qualifiedName ?: c.kotlin.jvmName
        return getLogger(name)
    }
    fun getLogger(clazz: KClass<*>): LoggerUtil = getLogger(clazz.java)

    @JvmName("getLoggerInline")
    inline fun <reified T> getLogger(): LoggerUtil = getLogger(T::class)

    @CallerSensitive
    @OptIn(KallerSensitive::class)
    fun getLogger(): LoggerUtil = getCallerClass()?.let(MyDeepSeekLogger::getLogger) ?: globalLogger
    internal val nativeOut: PrintStream = System.out
    internal val nativeErr: PrintStream = System.err

    /**
     * logger中的日期格式
     */
    val loggerDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    /**
     * 日志输出流
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val out: PrintStream = PrintStream(LoggerOutputStream(Level.INFO))

    /**
     * 日志错误流
     */
    val err: PrintStream = PrintStream(LoggerOutputStream(Level.SEVERE))
    fun addFilter(pattern: String)
    {
        loggerConfig = loggerConfig.copy(matchers = loggerConfig.matchers + pattern)
    }

    fun removeFilter(pattern: String)
    {
        loggerConfig = loggerConfig.copy(matchers = loggerConfig.matchers.filter { it != pattern })
    }

    fun setWhiteList(whiteList: Boolean)
    {
        loggerConfig = loggerConfig.copy(whiteList = whiteList)
    }

    fun setLevel(level: Level)
    {
        loggerConfig = loggerConfig.copy(levelName = level.name)
    }

    /**
     * 获取过滤器
     */
    fun filters(): MutableList<String> = Collections.unmodifiableList(loggerConfig.matchers)

    /**
     * 若由于终端相关组件错误导致的异常, 异常可能最终被捕获并打印在终端上, 可能导致再次抛出错误, 最终引起[StackOverflowError].
     * 未避免此类问题, 在涉及需要打印内容的地方, 应使用此方法.
     * 此当[block]出现错误时, 将绕过终端相关组件, 直接打印在标准输出流上. 以避免[StackOverflowError]的发生.
     */
    internal inline fun safe(block: ()->Unit)
    {
        runCatching(block).onFailure()
        {
            it.printStackTrace(nativeErr)
        }
    }

    /**
     * 初始化logger，将设置终端支持显示颜色码，捕获stdout，应在启动springboot前调用
     */
    init
    {
        System.setOut(out)
        System.setErr(err)
        globalLogger.logger.setUseParentHandlers(false)
        globalLogger.logger.handlers.forEach(globalLogger.logger::removeHandler)
        globalLogger.logger.addHandler(ToConsoleHandler)
        globalLogger.logger.addHandler(ToFileHandler)
    }

    private class LoggerOutputStream(private val level: Level): OutputStream()
    {
        val arrayOutputStream = ByteArrayOutputStream()

        @CallerSensitive
        @OptIn(KallerSensitive::class)
        override fun write(b: Int) = safe()
        {
            if (b == '\n'.code)
            {
                val str: String
                synchronized(arrayOutputStream)
                {
                    str = arrayOutputStream.toString()
                    arrayOutputStream.reset()
                }
                getCallerClasses().stream()
                    .filter {
                        !(it.packageName.startsWith("java") ||
                          it.packageName.startsWith("kotlin") ||
                          it.packageName.startsWith("jdk") ||
                          it.packageName.startsWith("sun"))

                    }
                    .findFirst()
                    .map(MyDeepSeekLogger::getLogger)
                    .getOrDefault(getLogger()).logger.log(level, str)
            }
            else synchronized(arrayOutputStream) { arrayOutputStream.write(b) }
        }
    }
}

/**
 * 向终端中打印log
 */
object ToConsoleHandler: Handler()
{
    init
    {
        this.formatter = object: Formatter()
        {
            override fun format(record: LogRecord): String
            {
                val message = formatMessage(record)
                val messages = mutableListOf(message)
                if (record.thrown != null)
                {
                    val str = record.thrown.stackTraceToString()
                    str.split("\n").forEach { messages.add(it) }
                }
                val level = record.level
                val ansiStyle = if (level.intValue() >= Level.SEVERE.intValue()) SimpleAnsiColor.RED.bright()
                else if (level.intValue() >= Level.WARNING.intValue()) SimpleAnsiColor.YELLOW.bright()
                else if (level.intValue() >= Level.CONFIG.intValue()) SimpleAnsiColor.BLUE.bright()
                else SimpleAnsiColor.GREEN.bright()
                val head = if (loggerConfig.showLoggerName) String.format(
                    "%s[%s]%s[%s]%s[%s]%s",
                    PURPLE.bright(),
                    MyDeepSeekLogger.loggerDateFormat.format(record.millis),
                    CYAN.bright(),
                    record.loggerName,
                    ansiStyle,
                    level.name,
                    RESET,
                )
                else String.format(
                    "%s[%s]%s[%s]%s",
                    PURPLE.bright(),
                    MyDeepSeekLogger.loggerDateFormat.format(record.millis),
                    ansiStyle,
                    level.name,
                    RESET,
                )
                return messages.joinToString("\n") { "$head $it$RESET" }
            }
        }
    }

    override fun publish(record: LogRecord) = safe()
    {
        if (!loggerConfig.check(record)) return
        val message = formatter.format(record)
        /**
         * 当[Console.println]调用的时候, 向终端打印日志, 会调用jline库,
         * 使得[org.jline.utils.StyleResolver]会在此时打印等级为FINEST的日志.
         *
         * 当该日志打印时会调用[Console.println], 再次引起日志打印, 造成无限递归.
         * 因此在这里特别处理
         */
        if (record.loggerName.startsWith("org.jline")) return

        Console.println(message)
    }

    override fun flush() = Unit
    override fun close() = Unit
}

/**
 * 将log写入文件
 */
object ToFileHandler: Handler()
{
    /**
     * log文件的日期格式
     */
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")

    /**
     * log文件的目录
     */
    private val logDir = File(workDir, "logs")

    /**
     * log文件
     */
    private val logFile = File(logDir, "latest.log")

    /**
     * 当前log文件的行数
     */
    private var cnt = 0

    init
    {
        new()
        this.formatter = object: Formatter()
        {
            override fun format(record: LogRecord): String
            {
                val message = formatMessage(record)
                val messages = mutableListOf(message)
                if (record.thrown != null)
                {
                    val str = record.thrown.stackTraceToString()
                    str.split("\n").forEach { messages.add(it) }
                }
                val level = record.level
                val head = if (loggerConfig.showLoggerName) String.format(
                    "[%s][%s][%s]",
                    MyDeepSeekLogger.loggerDateFormat.format(record.millis),
                    record.loggerName,
                    level.name
                )
                else String.format(
                    "[%s][%s]",
                    MyDeepSeekLogger.loggerDateFormat.format(record.millis),
                    level.name
                )
                return messages.joinToString("\n") { "$head $it" }
            }
        }
    }

    /**
     * 创建新的log文件
     */
    private fun new()
    {
        if (!logDir.exists()) logDir.mkdirs()
        if (logFile.exists()) // 如果文件已存在，则压缩到zip
        { // 将已有的log压缩到zip
            val zipFile = File(logDir, "${fileDateFormat.format(System.currentTimeMillis())}.zip")
            zipFile(zipFile)
            logFile.delete()
        }
        logFile.createNewFile() // 创建新的log文件
        cnt = 0 // 重置行数
    }

    /**
     * 将log文件压缩到zip
     */
    private fun zipFile(zipFile: File)
    {
        if (!logFile.exists()) return
        if (!zipFile.exists()) zipFile.createNewFile()
        val fos = FileOutputStream(zipFile)
        val zipOut = ZipOutputStream(fos)
        val fis = FileInputStream(logFile)
        val zipEntry = ZipEntry(logFile.getName())
        zipOut.putNextEntry(zipEntry)
        val bytes = ByteArray(1024)
        var length: Int
        while (fis.read(bytes).also { length = it } >= 0) zipOut.write(bytes, 0, length)
        zipOut.close()
        fis.close()
        fos.close()
    }

    fun clearOld(duration: Duration = loggerConfig.logFileSaveTime)
    {
        val files = logDir.listFiles() ?: return
        files.asSequence()
            .filter { it.name.endsWith(".zip") }
            .map { it to it.name.substringBeforeLast(".zip") }
            .map { runCatching { it.first to fileDateFormat.parse(it.second) }.getOrNull() }
            .filterNotNull()
            .map { it.first to it.second.toInstant().toKotlinInstant() }
            .map { it.first to (Clock.System.now() - it.second) }
            .filter { it.second > duration }
            .map { it.first }
            .forEach { it.delete() }
    }

    private fun check()
    {
        if ((cnt ushr 10) > 0)
        {
            new()
            clearOld()
        }
    }

    private fun append(lines: List<String>) = synchronized(this)
    {
        if (!logFile.exists()) new()
        val writer = FileWriter(logFile, true)
        lines.forEach { writer.appendLine(it) }
        writer.close()
        check()
    }

    private val colorMatcher = Regex("\u001B\\[[;\\d]*m")
    override fun publish(record: LogRecord) = safe()
    {
        if (!loggerConfig.check(record)) return
        val message = formatter.format(record)
        val messagesWithOutColor = colorMatcher.replace(message, "")
        append(messagesWithOutColor.split("\n"))
    }

    override fun flush() = Unit
    override fun close() = Unit
}