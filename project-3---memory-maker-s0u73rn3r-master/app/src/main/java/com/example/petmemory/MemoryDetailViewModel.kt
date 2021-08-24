package com.example.petmemory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*

class MemoryDetailViewModel() : ViewModel() {
    private val memoryRepository = MemoryRepository.get()
    private val memoryID = MutableLiveData<UUID>()

    var memoryData: LiveData<Memory?> = Transformations.switchMap(memoryID) {
            memoryID -> memoryRepository.getMemory(memoryID)
    } // this will only happen IF the data is changed

    fun loadMemory(newMemoryID: UUID) {
        memoryID.value = newMemoryID
    }

    fun saveMemory(memory: Memory) {
        memoryRepository.updateMemory(memory)
    }

    fun getPhotoFile(memory: Memory): File {
        return memoryRepository.getPhotoFile(memory)
    }
}