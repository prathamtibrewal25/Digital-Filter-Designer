package com.example.data

import androidx.room.*
import com.example.model.FilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterDao {
    @Query("SELECT * FROM saved_filters ORDER BY timestamp DESC")
    fun getAllFilters(): Flow<List<FilterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilter(filter: FilterEntity): Long

    @Query("DELETE FROM saved_filters WHERE id = :id")
    suspend fun deleteFilterById(id: Long)
}
