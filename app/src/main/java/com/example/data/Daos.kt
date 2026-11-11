package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM recent_documents ORDER BY lastOpenedTimestamp DESC")
    fun getRecentDocuments(): Flow<List<RecentDocumentEntity>>

    @Query("SELECT * FROM recent_documents WHERE isFavorite = 1 ORDER BY lastOpenedTimestamp DESC")
    fun getFavoriteDocuments(): Flow<List<RecentDocumentEntity>>

    @Query("SELECT * FROM recent_documents ORDER BY name ASC")
    fun getAllDocuments(): Flow<List<RecentDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(doc: RecentDocumentEntity)

    @Query("UPDATE recent_documents SET isFavorite = :isFav WHERE uri = :uri")
    suspend fun toggleFavorite(uri: String, isFav: Boolean)

    @Query("UPDATE recent_documents SET lastPage = :page, readingProgress = :progress, lastOpenedTimestamp = :timestamp WHERE uri = :uri")
    suspend fun updateReadingProgress(uri: String, page: Int, progress: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE recent_documents SET name = :newName WHERE uri = :uri")
    suspend fun renameDocument(uri: String, newName: String)

    @Query("DELETE FROM recent_documents WHERE uri = :uri")
    suspend fun deleteDocument(uri: String)
}

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM app_preferences WHERE id = 1 LIMIT 1")
    fun getPreferences(): Flow<AppPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(pref: AppPreferencesEntity)
}
