package com.example.gpsspeedometer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.util.*

data class TripRecord(
    val id: String = UUID.randomUUID().toString(),
    val topSpeed: Int,
    val avgSpeed: Int,
    val timestamp: Long = System.currentTimeMillis()
)

class SpeedometerViewModel : ViewModel() {
    var currentSpeed by mutableStateOf(0)
    var topSpeed by mutableStateOf(0)
    var avgSpeed by mutableStateOf(0)
    var showSummary by mutableStateOf(false)
    var isSimulating = false

    val speedRecords = mutableListOf<Int>()
    val tripHistory = mutableStateListOf<TripRecord>()

    fun updateSpeed(speed: Int) {
        currentSpeed = speed
        if (speed > 0) {
            showSummary = false
            speedRecords.add(speed)
            if (speed > topSpeed) {
                topSpeed = speed
            }
        }
    }

    fun calculateSummary() {
        if (speedRecords.isNotEmpty()) {
            avgSpeed = speedRecords.average().toInt()
            saveCurrentTripToHistory()
        }
        showSummary = true
        
        // Clear current trip stats so the next movement starts a fresh trip
        topSpeed = 0
        avgSpeed = 0
        speedRecords.clear()
    }

    fun resetStats() {
        saveCurrentTripToHistory()
        
        topSpeed = 0
        avgSpeed = 0
        speedRecords.clear()
        showSummary = false
    }

    private fun saveCurrentTripToHistory() {
        if (topSpeed > 0 || speedRecords.isNotEmpty()) {
            val record = TripRecord(
                topSpeed = topSpeed,
                avgSpeed = if (speedRecords.isNotEmpty()) speedRecords.average().toInt() else 0
            )
            tripHistory.add(0, record) // Add to top of list
        }
    }

    fun clearHistory() {
        tripHistory.clear()
    }
}
