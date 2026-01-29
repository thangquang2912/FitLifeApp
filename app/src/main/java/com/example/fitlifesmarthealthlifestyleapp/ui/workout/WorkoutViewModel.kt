package com.example.fitlifesmarthealthlifestyleapp.ui.workout

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitlifesmarthealthlifestyleapp.data.repository.WorkoutRepository
import com.example.fitlifesmarthealthlifestyleapp.domain.model.WorkoutProgram
import kotlinx.coroutines.launch

class WorkoutViewModel : ViewModel() {
    private val repository = WorkoutRepository()

    // Danh sách gốc (full) lưu trong RAM
    private var fullProgramList: List<WorkoutProgram> = emptyList()

    // Danh sách hiển thị ra màn hình (đã lọc)
    private val _displayPrograms = MutableLiveData<List<WorkoutProgram>>()
    val displayPrograms: LiveData<List<WorkoutProgram>> = _displayPrograms

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadPrograms() {
        if (fullProgramList.isNotEmpty()) {
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getWorkoutPrograms()
            if (result.isSuccess) {
                fullProgramList = result.getOrDefault(emptyList())
                // Mặc định hiển thị tất cả
                _displayPrograms.value = fullProgramList
            }
            _isLoading.value = false
        }
    }

    // Logic lọc cực đơn giản
    fun filterPrograms(category: String) {
        if (category == WorkoutCategory.ALL ) {
            _displayPrograms.value = fullProgramList
        } else {
            // Lọc theo category (Không phân biệt hoa thường)
            val filtered = fullProgramList.filter {
                it.category.equals(category, ignoreCase = true)
            }
            _displayPrograms.value = filtered
        }
    }
}