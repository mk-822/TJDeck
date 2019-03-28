package net.totoraj.tjdeck.model.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class AccountEntity(@PrimaryKey val userId: Long) {
    @ColumnInfo
    var token: String? = ""

    @ColumnInfo
    var tokenSecret: String = ""

    @ColumnInfo
    var iconUrl: String = ""

    @ColumnInfo
    var isDefaultUser: Boolean = false

    fun clone(): AccountEntity = AccountEntity(userId).also {
        it.token = token
        it.tokenSecret = tokenSecret
        it.iconUrl = iconUrl
        it.isDefaultUser = isDefaultUser
    }

    override fun equals(other: Any?): Boolean {
        if (other !is AccountEntity) return false
        return userId == other.userId
    }

    override fun hashCode(): Int = userId.toInt()
}