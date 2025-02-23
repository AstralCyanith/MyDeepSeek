package moe.tachyon.database

import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import moe.tachyon.JWTAuth
import moe.tachyon.dataClass.Permission
import moe.tachyon.database.utils.singleOrNull
import moe.tachyon.dataClass.UserFull
import moe.tachyon.dataClass.UserId
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone

class Users: SqlDao<Users.UserTable>(UserTable)
{
    /**
     * 用户信息表
     */
    object UserTable: IdTable<UserId>("users")
    {
        override val id = userId("id").autoIncrement().entityId()
        val username = varchar("username", 100).uniqueIndex()
        val permission = enumerationByName<Permission>("permission", 20).default(Permission.NORMAL)
        val password = text("password")
        val lastPasswordChange = timestampWithTimeZone("last_password_change").defaultExpression(
            CurrentTimestampWithTimeZone
        )
        override val primaryKey = PrimaryKey(id)
    }

    private fun deserialize(row: ResultRow) = UserFull(
        id = row[UserTable.id].value,
        username = row[UserTable.username],
        permission = row[UserTable.permission],
    )

    suspend fun createUser(
        username: String,
        password: String,
        permission: Permission,
    ): UserId = query()
    {
        val psw = JWTAuth.encryptPassword(password) // 加密密码
        insertAndGetId {
            it[UserTable.username] = username
            it[UserTable.password] = psw
            it[UserTable.permission] = permission
        }.value
    }

    suspend fun setUsername(id: UserId, username: String): Boolean = query()
    {
        update({ UserTable.id eq id }) { it[UserTable.username] = username } > 0
    }

    suspend fun getUser(id: UserId): UserFull? = query()
    {
        selectAll().where { UserTable.id eq id }.singleOrNull()?.let(::deserialize)
    }

    suspend fun getUserByUsername(username: String): UserFull? = query()
    {
        selectAll().where { UserTable.username eq username }.singleOrNull()?.let(::deserialize)
    }

    suspend fun setPassword(id: UserId, password: String): Boolean = query()
    {
        val psw = JWTAuth.encryptPassword(password) // 加密密码
        update({ UserTable.id eq id }) {
            it[UserTable.password] = psw
            it[lastPasswordChange] = CurrentTimestampWithTimeZone
        } > 0
    }

    /**
     * 获取某一用户的数据及其上次密码修改时间
     */
    suspend fun getUserWithLastPasswordChange(id: UserId): Pair<UserFull, Instant>? = query()
    {
        selectAll().where { UserTable.id eq id }.singleOrNull()?.let {
            deserialize(it) to it[lastPasswordChange].toInstant().toKotlinInstant()
        }
    }

    /**
     * 检查用户密码是否正确
     * @param userId 用户id
     * @param password 密码
     * @return 密码是否正确
     */
    suspend fun checkLogin(userId: UserId, password: String): Boolean = query()
    {
        val psw = select(table.password).where { id eq userId }.singleOrNull()?.get(table.password) ?: return@query false
        return@query JWTAuth.verifyPassword(password, psw)
    }

    /**
     * 检查用户密码是否正确
     * @param username 用户昵称
     * @param password 密码
     * @return 当用户不存在或密码错误时返回null, 否则返回用户id
     */
    suspend fun checkLogin(username: String, password: String): UserId? = query()
    {
        return@query getUserByUsername(username)?.let { user -> user.id.takeIf { checkLogin(it, password) } }
    }
}