package net.totoraj.tjdeck.model.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity
class AccountEntity(@PrimaryKey val userId: Long) : Serializable {
    @ColumnInfo
    var token: String? = ""

    @ColumnInfo
    var tokenSecret: String = ""

    @ColumnInfo
    var iconUrl: String = ""

    @ColumnInfo
    var isDefaultUser: Boolean = false
}