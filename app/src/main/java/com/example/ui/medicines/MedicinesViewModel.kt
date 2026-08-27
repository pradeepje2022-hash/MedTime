package com.example.ui.medicines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MedTimeApplication
import com.example.model.Medicine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MedicineFilter {
    ALL,
    ACTIVE,
    PAUSED
}

class MedicinesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MedTimeApplication
    private val repository = app.medicineRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(MedicineFilter.ALL)
    val filter: StateFlow<MedicineFilter> = _filter.asStateFlow()

    val medicines: StateFlow<List<Medicine>> = combine(
        repository.allMedicines,
        _searchQuery,
        _filter
    ) { list, query, filterMode ->
        list.filter { med ->
            val matchesQuery = query.isBlank() || med.name.contains(query, ignoreCase = true) ||
                    med.fullDosage.contains(query, ignoreCase = true) ||
                    med.notes.contains(query, ignoreCase = true)

            val matchesFilter = when (filterMode) {
                MedicineFilter.ALL -> true
                MedicineFilter.ACTIVE -> med.isActive
                MedicineFilter.PAUSED -> !med.isActive
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(newFilter: MedicineFilter) {
        _filter.value = newFilter
    }

    fun toggleMedicineActive(medicine: Medicine) {
        viewModelScope.launch {
            repository.toggleActiveStatus(medicine)
        }
    }

    fun deleteMedicine(medicine: Medicine) {
        viewModelScope.launch {
            repository.deleteMedicine(medicine)
        }
    }

    fun clearAllMedicines() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }
}
