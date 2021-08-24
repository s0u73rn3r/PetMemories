package com.example.petmemory

import android.app.Application

class PetMemoriesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MemoryRepository.initialize(this)
    }
}