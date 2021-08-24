package com.example.petmemory.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.petmemory.Memory

@Database(entities = [ Memory::class ], version=1)
@TypeConverters(MemoryTypeConvertors::class)
abstract class MemoryDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
}