package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.FilterRepository
import com.example.dsp.*
import com.example.model.FilterEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FilterViewModel(private val repository: FilterRepository) : ViewModel() {

    // Inputs state
    private val _sampleRate = MutableStateFlow(48000.0)
    val sampleRate = _sampleRate.asStateFlow()

    private val _filterMethod = MutableStateFlow(FilterMethod.FIR_WINDOW)
    val filterMethod = _filterMethod.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.LOWPASS)
    val filterType = _filterType.asStateFlow()

    private val _fc1 = MutableStateFlow(4000.0)
    val fc1 = _fc1.asStateFlow()

    private val _fc2 = MutableStateFlow(12000.0)
    val fc2 = _fc2.asStateFlow()

    private val _firTaps = MutableStateFlow(31)
    val firTaps = _firTaps.asStateFlow()

    private val _windowType = MutableStateFlow(WindowType.HAMMING)
    val windowType = _windowType.asStateFlow()

    private val _iirOrder = MutableStateFlow(4)
    val iirOrder = _iirOrder.asStateFlow()

    private val _chebyRippleDb = MutableStateFlow(1.0)
    val chebyRippleDb = _chebyRippleDb.asStateFlow()

    // Validation error state
    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    // Active design results state
    private val _activeResult = MutableStateFlow<FilterResult?>(null)
    val activeResult = _activeResult.asStateFlow()

    // List of saved filters from persistence
    val savedFilters: StateFlow<List<FilterEntity>> = repository.allFilters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Trigger initial calculation
        triggerCalculation()
    }

    // Input Mutations
    private fun clampFrequencies() {
        val nyquist = _sampleRate.value / 2.0
        val maxFc1 = maxOf(2.0, nyquist - 10.0)
        _fc1.value = _fc1.value.coerceIn(1.0, maxFc1)
        
        val minFc2 = _fc1.value + 10.0
        val maxFc2 = maxOf(minFc2 + 1.0, nyquist - 1.0)
        _fc2.value = _fc2.value.coerceIn(minFc2, maxFc2)
    }

    fun setSampleRate(rate: Double) {
        _sampleRate.value = maxOf(100.0, rate)
        clampFrequencies()
        validateAndRecalculate()
    }

    fun setFilterMethod(method: FilterMethod) {
        _filterMethod.value = method
        clampFrequencies()
        validateAndRecalculate()
    }

    fun setFilterType(type: FilterType) {
        _filterType.value = type
        clampFrequencies()
        validateAndRecalculate()
    }

    fun setFc1(freq: Double) {
        val nyquist = _sampleRate.value / 2.0
        val maxFc1 = maxOf(2.0, nyquist - 10.0)
        _fc1.value = freq.coerceIn(1.0, maxFc1)
        clampFrequencies()
        validateAndRecalculate()
    }

    fun setFc2(freq: Double) {
        val nyquist = _sampleRate.value / 2.0
        val minFc2 = _fc1.value + 10.0
        val maxFc2 = maxOf(minFc2 + 1.0, nyquist - 1.0)
        _fc2.value = freq.coerceIn(minFc2, maxFc2)
        clampFrequencies()
        validateAndRecalculate()
    }

    fun setFirTaps(taps: Int) {
        // Must be in [3, 255] and odd taps are generally preferred for type I/II FIR filters
        _firTaps.value = taps.coerceIn(3, 255)
        validateAndRecalculate()
    }

    fun setWindowType(window: WindowType) {
        _windowType.value = window
        validateAndRecalculate()
    }

    fun setIirOrder(order: Int) {
        // Keep order even (2, 4, 6, 8)
        _iirOrder.value = order.coerceIn(2, 8)
        validateAndRecalculate()
    }

    fun setChebyRippleDb(ripple: Double) {
        _chebyRippleDb.value = ripple.coerceIn(0.1, 10.0)
        validateAndRecalculate()
    }

    // Validation & Core DSP calculation trigger
    private fun validateAndRecalculate() {
        val fs = _sampleRate.value
        val nyquist = fs / 2.0
        val f1 = _fc1.value
        val f2 = _fc2.value
        val method = _filterMethod.value
        val type = _filterType.value

        if (fs <= 0.0) {
            _errorMsg.value = "Sampling rate must be positive"
            return
        }

        if (f1 <= 0.0 || f1 >= nyquist) {
            _errorMsg.value = "Cutoff 1 (%.1f Hz) must be between 0 and Nyquist (%.1f Hz)".format(f1, nyquist)
            return
        }

        if ((type == FilterType.BANDPASS || type == FilterType.BANDSTOP)) {
            if (f2 <= 0.0 || f2 >= nyquist) {
                _errorMsg.value = "Cutoff 2 (%.1f Hz) must be between 0 and Nyquist (%.1f Hz)".format(f2, nyquist)
                return
            }
            if (f1 >= f2) {
                _errorMsg.value = "Cutoff 1 must be strictly less than Cutoff 2 for multiplex designs"
                return
            }
        }

        // All clear
        _errorMsg.value = null
        triggerCalculation()
    }

    private fun triggerCalculation() {
        try {
            val result = FilterDesigner.generateResult(
                method = _filterMethod.value,
                type = _filterType.value,
                sampleRate = _sampleRate.value,
                fc1 = _fc1.value,
                fc2 = _fc2.value,
                firTaps = _firTaps.value,
                windowType = _windowType.value,
                iirOrder = _iirOrder.value,
                chebyRippleDb = _chebyRippleDb.value
            )
            _activeResult.value = result
        } catch (e: Exception) {
            _errorMsg.value = "DSP Calculation Error: ${e.message}"
        }
    }

    // Database Actions
    fun saveCurrentFilter(customLabel: String) {
        viewModelScope.launch {
            val label = customLabel.trimmedOrPlaceholder()
            val entity = FilterEntity(
                label = label,
                method = _filterMethod.value.name,
                type = _filterType.value.name,
                sampleRate = _sampleRate.value,
                fc1 = _fc1.value,
                fc2 = _fc2.value,
                firTaps = _firTaps.value,
                windowType = _windowType.value.name,
                iirOrder = _iirOrder.value,
                chebyRippleDb = _chebyRippleDb.value
            )
            repository.insert(entity)
        }
    }

    fun loadSavedFilter(entity: FilterEntity) {
        _filterMethod.value = FilterMethod.valueOf(entity.method)
        _filterType.value = FilterType.valueOf(entity.type)
        _sampleRate.value = entity.sampleRate
        _fc1.value = entity.fc1
        _fc2.value = entity.fc2
        _firTaps.value = entity.firTaps
        _windowType.value = WindowType.valueOf(entity.windowType)
        _iirOrder.value = entity.iirOrder
        _chebyRippleDb.value = entity.chebyRippleDb

        validateAndRecalculate()
    }

    fun deleteSavedFilter(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    private fun String.trimmedOrPlaceholder(): String {
        val trimmed = this.trim()
        if (trimmed.isNotEmpty()) return trimmed
        return "Saved Design (${_filterType.value.name.lowercase().replaceFirstChar { it.uppercase() }})"
    }

    companion object {
        fun provideFactory(repository: FilterRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FilterViewModel(repository) as T
                }
            }
    }
}
