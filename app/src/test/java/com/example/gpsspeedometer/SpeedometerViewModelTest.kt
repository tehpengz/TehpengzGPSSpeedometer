package com.example.gpsspeedometer

import org.junit.Assert.*
import org.junit.Test

class SpeedometerViewModelTest {

    @Test
    fun `resetStats adds current trip to history`() {
        val viewModel = SpeedometerViewModel()
        
        // Simulate a trip
        viewModel.updateSpeed(60)
        viewModel.updateSpeed(100)
        viewModel.updateSpeed(80)
        
        assertEquals(100, viewModel.topSpeed)
        
        // Reset and save
        viewModel.resetStats()
        
        assertEquals(0, viewModel.topSpeed)
        assertEquals(1, viewModel.tripHistory.size)
        assertEquals(100, viewModel.tripHistory[0].topSpeed)
        assertEquals(80, viewModel.tripHistory[0].avgSpeed)
    }

    @Test
    fun `calculateSummary adds current trip to history and clears current stats`() {
        val viewModel = SpeedometerViewModel()
        
        // Simulate a trip
        viewModel.updateSpeed(40)
        viewModel.updateSpeed(60)
        
        // Auto-save via calculateSummary
        viewModel.calculateSummary()
        
        assertEquals(1, viewModel.tripHistory.size)
        assertEquals(60, viewModel.tripHistory[0].topSpeed)
        assertEquals(50, viewModel.tripHistory[0].avgSpeed)
        
        // Verify current stats are reset
        assertEquals(0, viewModel.topSpeed)
        assertEquals(0, viewModel.avgSpeed)
        assertTrue(viewModel.showSummary)
    }
}
