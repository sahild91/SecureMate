package com.example.securemate.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.securemate.flagged_links_logger.FlaggedLink
import com.example.securemate.flagged_links_logger.FlaggedLinkDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SuspiciousLinksViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = FlaggedLinkDatabase.getInstance(application).linkDao()

    private val _links = MutableStateFlow<List<FlaggedLink>>(emptyList())
    val links: StateFlow<List<FlaggedLink>> = _links

    fun loadLinks() {
        viewModelScope.launch {
            _links.value = dao.getAll()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            dao.clearAll()
            _links.value = emptyList()
        }
    }
}