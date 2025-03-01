package moe.tachyon.console.command

import org.jline.reader.Candidate
import moe.tachyon.config.loggerConfig
import moe.tachyon.logger.MyDeepSeekLogger

/**
 * Logger control.
 */
object Logger : TreeCommand(Level, Filter, ShowLoggerName)
{
    override val description = "Logger control."

    /**
     * Set/get logger level.
     */
    object Level : Command
    {
        override val description = "Set/get logger level."
        override val args = "[level]"

        override suspend fun execute(sender: CommandSet.CommandSender, args: List<String>): Boolean
        {
            if (args.isEmpty()) // 没参数就打印当前日志等级
            {
                sender.out("logger level: ${MyDeepSeekLogger.globalLogger.logger.level.name}")
            }
            else try
            {
                // 有参数就设置日志等级
                val level=java.util.logging.Level.parse(args[0])
                MyDeepSeekLogger.setLevel(level)
                sender.out("set logger level to ${level.name}")
            }
            catch (e: IllegalArgumentException) // 输入的日志等级不合法
            {
                sender.err("Unknown level: ${args[0]}")
            }
            return true
        }

        override suspend fun tabComplete(args: List<String>): List<Candidate>
        {
            if (args.size==1)
            {
                return listOf("OFF", "SEVERE", "WARNING", "INFO", "CONFIG", "FINE", "FINER", "FINEST", "ALL").map { Candidate(it) }
            }
            return listOf()
        }
    }

    /**
     * 日志过滤器相关
     */
    object Filter : TreeCommand(Add, Remove, List, Mode)
    {
        override val description = "Filter control."

        /**
         * 添加一个过滤规则
         */
        object Add : Command
        {
            override val description = "Add a filter."
            override val args = "<pattern>"

            override suspend fun execute(
                sender: CommandSet.CommandSender,
                args: kotlin.collections.List<String>
            ): Boolean
            {
                if (args.isEmpty())
                {
                    sender.err("No filter specified.")
                    return true
                }
                MyDeepSeekLogger.addFilter(args[0])
                sender.out("Added filter: ${args[0]}")
                return true
            }
        }

        /**
         * 删除一个过滤规则
         */
        object Remove : Command
        {
            override val description = "Remove a filter."
            override val args = "<pattern>"
            override val aliases = listOf("rm")

            override suspend fun execute(
                sender: CommandSet.CommandSender,
                args: kotlin.collections.List<String>
            ): Boolean
            {
                if (args.isEmpty())
                {
                    sender.err("No filter specified.")
                    return true
                }
                MyDeepSeekLogger.removeFilter(args[0])
                sender.out("Removed filter: ${args[0]}")
                return true
            }

            override suspend fun tabComplete(args: kotlin.collections.List<String>): kotlin.collections.List<Candidate>
            {
                if (args.size==1)
                {
                    return MyDeepSeekLogger.filters().map { Candidate(it) }
                }
                return listOf()
            }
        }

        /**
         * 列出所有过滤规则
         */
        object List : Command
        {
            override val description = "List all filters."
            override val aliases = listOf("ls")

            override suspend fun execute(
                sender: CommandSet.CommandSender,
                args: kotlin.collections.List<String>
            ): Boolean
            {
                sender.out("Filters:")
                for (filter in MyDeepSeekLogger.filters())
                {
                    sender.out("- $filter")
                }
                return true
            }
        }

        /**
         * 设置过滤器模式(白名单/黑名单)
         */
        object Mode : Command
        {
            override val description = "Set/get filter mode."
            override val args = "[mode]"

            override suspend fun execute(
                sender: CommandSet.CommandSender,
                args: kotlin.collections.List<String>
            ): Boolean
            {
                if (args.isEmpty())
                {
                    sender.out("filter mode: ${if (loggerConfig.whiteList) "whitelist" else "blacklist"}")
                }
                else
                {
                    when (args[0])
                    {
                        "whitelist" ->
                        {
                            MyDeepSeekLogger.setWhiteList(true)
                            sender.out("set filter mode to whitelist")
                        }
                        "blacklist" ->
                        {
                            MyDeepSeekLogger.setWhiteList(false)
                            sender.out("set filter mode to blacklist")
                        }
                        else ->
                        {
                            sender.out("Unknown mode: ${args[0]}")
                            return true
                        }
                    }
                }
                return true
            }

            override suspend fun tabComplete(args: kotlin.collections.List<String>): kotlin.collections.List<Candidate>
            {
                if (args.size==1)
                {
                    return listOf("whitelist", "blacklist").map { Candidate(it) }
                }
                return listOf()
            }
        }
    }

    object ShowLoggerName : Command
    {
        override val description = "Set/get show logger name."
        override val args = "[true/false]"

        override suspend fun execute(sender: CommandSet.CommandSender, args: List<String>): Boolean
        {
            if (args.isEmpty())
            {
                sender.out("show logger name: ${loggerConfig.showLoggerName}")
                return true
            }
            if (args.size != 1) return false
            val show = args[0].toBooleanStrictOrNull() ?: return false
            loggerConfig = loggerConfig.copy(showLoggerName = show)
            return true
        }
        override suspend fun tabComplete(args: List<String>): List<Candidate>
        {
            return if (args.size == 1) listOf(Candidate("true"), Candidate("false"))
            else emptyList()
        }
    }
}