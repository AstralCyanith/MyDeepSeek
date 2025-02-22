package moe.tachyon.dataClass

import kotlinx.serialization.Serializable

@Serializable
data class UserFull(
    val id: UserId,
    val username: String,
    val permission: Permission,
)