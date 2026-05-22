package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_filters")
data class FilterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val method: String,       // e.g. "FIR_WINDOW", "IIR_BUTTERWORTH", "IIR_CHEBYSHEV"
    val type: String,         // e.g. "LOWPASS", "HIGHPASS", "BANDPASS", "BANDSTOP"
    val sampleRate: Double,
    val fc1: Double,
    val fc2: Double,
    val firTaps: Int,
    val windowType: String,   // e.g. "HAMMING", "BLACKMAN"
    val iirOrder: Int,
    val chebyRippleDb: Double,
    val timestamp: Long = System.currentTimeMillis()
)
