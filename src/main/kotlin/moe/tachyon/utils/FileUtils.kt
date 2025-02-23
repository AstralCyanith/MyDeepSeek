package moe.tachyon.utils

import moe.tachyon.dataClass.UserId
import moe.tachyon.dataDir
import moe.tachyon.logger.MyDeepSeekLogger
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * 头像工具类
 * 头像存储在本地, 按照用户ID给每个用户创建一个文件夹, 文件夹中存放用户的头像
 * 头像文件名为数字, 从0开始, 依次递增, 数字最大的即为当前使用的头像
 * 默认头像存放在 default 文件夹中, 可以在其中添加任意数量的头像, 用户被设置为默认头像时, 会随机选择一个头像
 */
object FileUtils
{
    private val logger = MyDeepSeekLogger.getLogger()
    private val avatarFolder = File(dataDir, "avatars")
    private val serverAvatarFolder = File(dataDir, "serverAvatars")
    private val defaultAvatarFolder = File(avatarFolder, "default")
    private const val AVATAR_SIZE = 512

    init
    {
        avatarFolder.mkdirs()
        serverAvatarFolder.mkdirs()
        defaultAvatarFolder.mkdirs()
    }

    private fun toString(int: Int) = int.toString(36).padStart(6, '0')

    private val emptyAvatar get() = BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB)

    fun setAvatar(user: UserId, avatar: BufferedImage) =
        setAvatar(File(avatarFolder, toString(user.value)), avatar)

    private fun setAvatar(folder: File, avatar: BufferedImage)
    {
        folder.mkdirs()
        // 文件夹中已有的头像数量
        val avatarCount = folder.listFiles()?.size ?: 0
        val avatarFile = File(folder, "${toString(avatarCount)}.png")
        avatarFile.createNewFile()
        // 将头像大小调整为 1024x1024
        val resizedAvatar = BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB)
        val graphics = resizedAvatar.createGraphics()
        graphics.drawImage(avatar, 0, 0, AVATAR_SIZE, AVATAR_SIZE, null)
        graphics.dispose()
        // 保存头像
        ImageIO.write(resizedAvatar, "png", avatarFile)
    }

    fun setDefaultAvatar(user: UserId): BufferedImage
    {
        val userAvatarFolder = File(avatarFolder, toString(user.value))
        userAvatarFolder.mkdirs()
        // 文件夹中已有的头像数量
        val avatarCount = userAvatarFolder.listFiles()?.size ?: 0
        val avatarFile = File(userAvatarFolder, "${toString(avatarCount)}.png")
        // 在默认头像文件夹中随机选择一个头像
        val defaultAvatarFiles = defaultAvatarFolder.listFiles()
        val defaultAvatar = defaultAvatarFiles?.randomOrNull()
        if (defaultAvatar == null)
        {
            logger.warning("No default avatar found")
            return emptyAvatar
        }
        // 保存头像
        defaultAvatar.copyTo(avatarFile)
        return ImageIO.read(defaultAvatar)
    }

    fun getAvatar(user: UserId): BufferedImage =
        getAvatar(File(avatarFolder, toString(user.value))) ?: setDefaultAvatar(user)

    private fun getAvatar(folder: File): BufferedImage?
    {
        val avatarCount = folder.listFiles()?.size ?: return null
        val avatarFile = File(folder, "${toString(avatarCount - 1)}.png")
        return if (avatarFile.exists()) ImageIO.read(avatarFile) else null
    }
}