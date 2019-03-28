package net.totoraj.tjdeck.model.database

import androidx.room.*
import net.totoraj.tjdeck.model.database.entity.AccountEntity

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entities: List<AccountEntity>)

    @Query("SELECT * FROM AccountEntity")
    suspend fun findAll(): List<AccountEntity>

    @Query("SELECT * FROM AccountEntity WHERE isDefaultUser = 1")
    suspend fun findDefaultAccount(): List<AccountEntity>

    @Query("SELECT * FROM AccountEntity WHERE userId = :userId")
    suspend fun findByUserId(userId: Long): List<AccountEntity>

    @Delete
    suspend fun delete(entities: List<AccountEntity>)

    @Query("DELETE FROM AccountEntity")
    suspend fun deleteAll()
}