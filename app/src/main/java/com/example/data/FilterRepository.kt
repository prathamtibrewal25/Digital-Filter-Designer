package com.example.data

import com.example.model.FilterEntity
import kotlinx.coroutines.flow.Flow

class FilterRepository(private val filterDao: FilterDao) {
    val allFilters: Flow<List<FilterEntity>> = filterDao.getAllFilters()

    suspend fun insert(filter: FilterEntity): Long {
        return filterDao.insertFilter(filter)
    }

    suspend fun deleteById(id: Long) {
        filterDao.deleteFilterById(id)
    }
}
