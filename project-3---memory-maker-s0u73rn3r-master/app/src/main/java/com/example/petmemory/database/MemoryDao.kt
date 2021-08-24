package com.example.petmemory.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.petmemory.Memory
import java.util.*

@Dao
interface MemoryDao {

    @Query("DELETE FROM memory")
    fun nukeTable()

    @Query("SELECT * FROM memory")
    fun getMemories() : LiveData<List<Memory>>

    @Query ("SELECT * FROM memory WHERE id=(:id)")
    fun getMemory(id: UUID) : LiveData<Memory?>

    @Update
    fun updateMemory(memory: Memory)

    @Insert
    fun addMemory(memory: Memory)

    @Delete
    fun deleteMemory(memory: Memory)
}