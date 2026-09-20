package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE user_id = :userId ORDER BY journal_date DESC, captured_at DESC")
    fun getAllPhotos(userId: String): Flow<List<DailyPhoto>>

    @Query("SELECT * FROM photos WHERE user_id = :userId AND journal_date = :journalDate ORDER BY captured_at ASC")
    fun getPhotosByDate(userId: String, journalDate: String): Flow<List<DailyPhoto>>

    @Query("SELECT * FROM photos WHERE user_id = :userId AND journal_date = :journalDate ORDER BY captured_at ASC")
    suspend fun getPhotosByDateSync(userId: String, journalDate: String): List<DailyPhoto>

    @Query("SELECT * FROM photos WHERE user_id = :userId AND journal_date = :journalDate ORDER BY captured_at ASC LIMIT 1")
    fun getPhotoByDate(userId: String, journalDate: String): Flow<DailyPhoto?>

    @Query("SELECT * FROM photos WHERE user_id = :userId AND journal_date = :journalDate ORDER BY captured_at ASC LIMIT 1")
    suspend fun getPhotoByDateSync(userId: String, journalDate: String): DailyPhoto?

    @Query("SELECT COUNT(*) FROM photos WHERE user_id = :userId AND journal_date = :journalDate")
    suspend fun getPhotoCountForDate(userId: String, journalDate: String): Int

    @Query("SELECT COUNT(*) FROM photos WHERE user_id = :userId AND journal_date = :journalDate")
    fun getPhotoCountForDateFlow(userId: String, journalDate: String): Flow<Int>

    @Query("SELECT * FROM photos WHERE user_id = :userId AND id = :id LIMIT 1")
    fun getPhotoById(userId: String, id: String): Flow<DailyPhoto?>

    @Query("SELECT * FROM photos WHERE user_id = :userId AND id = :id LIMIT 1")
    suspend fun getPhotoByIdSync(userId: String, id: String): DailyPhoto?

    @Query("SELECT * FROM photos WHERE user_id = :userId AND journal_date LIKE :yearMonth || '%' ORDER BY journal_date ASC, captured_at ASC")
    fun getPhotosForMonth(userId: String, yearMonth: String): Flow<List<DailyPhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(photo: DailyPhoto): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<DailyPhoto>): List<Long>

    @Delete
    suspend fun deletePhoto(photo: DailyPhoto)

    @Query("DELETE FROM photos WHERE user_id = :userId AND id = :id")
    suspend fun deletePhotoById(userId: String, id: String)

    @Query("DELETE FROM photos WHERE user_id = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT COUNT(*) FROM photos WHERE user_id = :userId")
    fun getTotalPhotosCount(userId: String): Flow<Int>
}
