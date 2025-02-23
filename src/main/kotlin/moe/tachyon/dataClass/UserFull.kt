package moe.tachyon.dataClass

import kotlinx.serialization.Serializable

@Serializable
data class UserFull(
    val id: UserId,
    val username: String,
    val permission: Permission,
)
{
    companion object
    {
        val example get() = UserFull(
            id = UserId(1),
            username = "NullAqua",
            permission = Permission.ADMIN,
        )
    }
    val hasAdmin get() = permission >= Permission.ADMIN
    val hasRoot get() = permission >= Permission.ROOT
}