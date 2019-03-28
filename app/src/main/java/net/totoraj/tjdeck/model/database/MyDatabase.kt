package net.totoraj.tjdeck.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.totoraj.tjdeck.model.database.entity.AccountEntity

@Database(entities = [(AccountEntity::class)], version = 1, exportSchema = false)
abstract class MyDatabase : RoomDatabase() {
    companion object {
        private const val dbName = "tjdeck_database.db"
        private lateinit var appDatabase: MyDatabase

        fun createDatabase(context: Context) {
            appDatabase = Room.databaseBuilder(context, MyDatabase::class.java, dbName).build()
        }

        fun getDatabase() = appDatabase
    }

    abstract val accountDao: AccountDao
}