package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CalculationHistory
import com.example.data.HistoryRepository
import com.example.domain.CalculationResult
import com.example.domain.ProcessCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = HistoryRepository(db.historyDao())

    // 1. UI State variables
    private val _mode = MutableStateFlow("forward") // "forward" or "backward"
    val mode: StateFlow<String> = _mode.asStateFlow()

    private val _inputHour = MutableStateFlow("")
    val inputHour: StateFlow<String> = _inputHour.asStateFlow()

    private val _inputMinute = MutableStateFlow("")
    val inputMinute: StateFlow<String> = _inputMinute.asStateFlow()

    private val _inputDuration = MutableStateFlow("")
    val inputDuration: StateFlow<String> = _inputDuration.asStateFlow()

    private val _calculationResult = MutableStateFlow<CalculationResult?>(null)
    val calculationResult: StateFlow<CalculationResult?> = _calculationResult.asStateFlow()

    // Observes the reactively updated Room history
    val historyList: StateFlow<List<CalculationHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _historyDrawerOpen = MutableStateFlow(false)
    val historyDrawerOpen: StateFlow<Boolean> = _historyDrawerOpen.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _dialogTitle = MutableStateFlow("알림")
    val dialogTitle: StateFlow<String> = _dialogTitle.asStateFlow()

    private val _dialogDesc = MutableStateFlow("")
    val dialogDesc: StateFlow<String> = _dialogDesc.asStateFlow()

    private val _showCancelButton = MutableStateFlow(false)
    val showCancelButton: StateFlow<Boolean> = _showCancelButton.asStateFlow()

    private var onDialogConfirmCallback: (() -> Unit)? = null

    // 2. Modifiers and events
    fun setMode(newMode: String) {
        _mode.value = newMode
        _calculationResult.value = null // clear result when shifting mode
    }

    fun updateHour(h: String) {
        // Validation helper for Hour range [0 .. 24]
        val clean = h.filter { it.isDigit() }
        if (clean.isEmpty()) {
            _inputHour.value = ""
            return
        }
        val num = clean.toIntOrNull() ?: 0
        if (num in 0..24) {
            _inputHour.value = num.toString()
        }
    }

    fun updateMinute(m: String) {
        // Validation helper for Minute range [0 .. 59]
        val clean = m.filter { it.isDigit() }
        if (clean.isEmpty()) {
            _inputMinute.value = ""
            return
        }
        val num = clean.toIntOrNull() ?: 0
        if (num in 0..59) {
            _inputMinute.value = num.toString()
        }
    }

    fun updateDuration(d: String) {
        // Durations must be positive digits.
        val clean = d.filter { it.isDigit() }
        if (clean.isEmpty()) {
            _inputDuration.value = ""
            return
        }
        val num = clean.toIntOrNull() ?: 1
        if (num in 1..1440) {
            _inputDuration.value = num.toString()
        }
    }

    fun setDurationValue(mins: Int) {
        _inputDuration.value = mins.toString()
    }

    fun toggleHistoryDrawer(open: Boolean) {
        _historyDrawerOpen.value = open
    }

    fun triggerDialog(title: String, desc: String, showCancel: Boolean, onConfirm: (() -> Unit)?) {
        _dialogTitle.value = title
        _dialogDesc.value = desc
        _showCancelButton.value = showCancel
        onDialogConfirmCallback = onConfirm
        _showDialog.value = true
    }

    fun dismissDialog(confirm: Boolean) {
        _showDialog.value = false
        if (confirm) {
            onDialogConfirmCallback?.invoke()
        }
        onDialogConfirmCallback = null
    }

    fun calculate() {
        val hStr = _inputHour.value
        val mStr = _inputMinute.value
        val dStr = _inputDuration.value

        if (hStr.isEmpty() || mStr.isEmpty() || dStr.isEmpty()) {
            triggerDialog("알림", "시간과 소요 시간을 모두 빈칸 없이 작성해주세요.", false, null)
            return
        }

        val h = hStr.toIntOrNull() ?: 0
        val m = mStr.toIntOrNull() ?: 0
        val d = dStr.toIntOrNull() ?: 0

        if (d <= 0) {
            triggerDialog("경고", "소요 시간은 1분 이상이어야 합니다.", false, null)
            return
        }

        val baseMin = ProcessCalculator.toMinutes(h, m)
        val result = if (_mode.value == "forward") {
            ProcessCalculator.calcForward(baseMin, d)
        } else {
            ProcessCalculator.calcBackward(baseMin, d)
        }

        _calculationResult.value = result

        // Persist history reactively in the local database
        viewModelScope.launch {
            repository.insert(
                CalculationHistory(
                    mode = _mode.value,
                    baseHour = h,
                    baseMin = m,
                    duration = d,
                    resultTimeStr = result.resultTimeStr,
                    totalBreakTime = result.totalBreakTime,
                    totalElapsed = result.totalElapsed
                )
            )
        }
    }

    fun reset() {
        _calculationResult.value = null
        _inputHour.value = ""
        _inputMinute.value = ""
        _inputDuration.value = ""
    }

    fun loadHistoryItem(item: CalculationHistory) {
        _mode.value = item.mode
        _inputHour.value = item.baseHour.toString()
        _inputMinute.value = item.baseMin.toString()
        _inputDuration.value = item.duration.toString()

        val baseMin = ProcessCalculator.toMinutes(item.baseHour, item.baseMin)
        val result = if (item.mode == "forward") {
            ProcessCalculator.calcForward(baseMin, item.duration)
        } else {
            ProcessCalculator.calcBackward(baseMin, item.duration)
        }

        _calculationResult.value = result
        _historyDrawerOpen.value = false // close bottom sheet
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        triggerDialog("전체 삭제", "기록된 계산 이력을 모두 비울까요?", true) {
            viewModelScope.launch {
                repository.clearAll()
            }
        }
    }
}
