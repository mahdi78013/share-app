package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.HistoryEntity
import com.example.data.SharingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class TabType {
    ALL, RECENT
}

enum class FilterType {
    ALL, USER, SYSTEM, LARGE
}

class ShareViewModel(
    application: Application,
    private val repository: SharingRepository
) : AndroidViewModel(application) {

    // Scanned apps state
    private val _scannedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // UI filters & controls
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(TabType.ALL)
    val selectedTab: StateFlow<TabType> = _selectedTab.asStateFlow()

    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSelectedTab(tab: TabType) {
        _selectedTab.value = tab
    }

    fun updateSelectedFilter(filter: FilterType) {
        _selectedFilter.value = filter
    }
    
    // Multi-select state (storing packageNames)
    private val _selectedAppPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedAppPackages: StateFlow<Set<String>> = _selectedAppPackages.asStateFlow()

    // Extraction state for UI loader
    private val _extractionProgress = MutableStateFlow<Float?>(null)
    val extractionProgress: StateFlow<Float?> = _extractionProgress.asStateFlow()

    private val _extractionMessage = MutableStateFlow<String?>(null)
    val extractionMessage: StateFlow<String?> = _extractionMessage.asStateFlow()

    // Room feeds
    val sharingHistory = repository.sharingHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Combined Flow of scanned apps filtered by search and selections
    val appsFlow: StateFlow<List<AppInfo>> = combine(
        _scannedApps,
        _searchQuery,
        _selectedFilter,
        _selectedTab
    ) { scanned, query, filter, tab ->
        // Filter based on tab selection
        val tabFiltered = when (tab) {
            TabType.ALL -> scanned
            TabType.RECENT -> emptyList() // Rendered separately from history list
        }

        // Apply filters (only for ALL tab display)
        val filterFiltered = when (filter) {
            FilterType.ALL -> tabFiltered
            FilterType.USER -> tabFiltered.filter { !it.isSystemApp }
            FilterType.SYSTEM -> tabFiltered.filter { it.isSystemApp }
            FilterType.LARGE -> tabFiltered.filter { it.apkSize >= 50 * 1024 * 1024 } // Over 50MB
        }

        // Apply Search query
        if (query.trim().isEmpty()) {
            filterFiltered
        } else {
            filterFiltered.filter {
                it.name.contains(query, ignoreCase = true) || 
                it.packageName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Scanner
    fun scanApps(context: Context, forceRefresh: Boolean = false) {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                // Background cleaning of stale extracted APK files
                repository.cleanStaleApks(context)
                
                val scanned = repository.scanInstalledApps(context, forceRefresh)
                _scannedApps.value = scanned
            } catch (e: Exception) {
                Toast.makeText(context, "Scanning failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isScanning.value = false
            }
        }
    }

    // Time updater Flow for local history representation, emitting time updates every 30 seconds
    val triggerTimeUpdate: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(30_000)
        }
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = System.currentTimeMillis()
    )

    // Toggle Multi-Selection
    fun toggleAppSelection(packageName: String) {
        val currentSet = _selectedAppPackages.value
        if (currentSet.contains(packageName)) {
            _selectedAppPackages.value = currentSet - packageName
        } else {
            _selectedAppPackages.value = currentSet + packageName
        }
    }

    fun clearAppSelection() {
        _selectedAppPackages.value = emptySet()
    }

    // History cleaning
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Core Extraction & Sharing engine - Single APK
    fun shareSingleApp(context: Context, app: AppInfo) {
        viewModelScope.launch {
            _extractionProgress.value = 0.0f
            _extractionMessage.value = "Extracting APK of ${app.name}..."
            try {
                val extractedFile = repository.extractApk(context, app) { progress ->
                    _extractionProgress.value = progress
                }
                
                // Record to local Room data
                repository.recordShare(app.packageName, app.name)

                // Trigger Sharing System
                triggerSystemShare(context, listOf(extractedFile), app.name)
            } catch (e: Exception) {
                Toast.makeText(context, "Extraction failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _extractionProgress.value = null
                _extractionMessage.value = null
            }
        }
    }

    // Core Extraction & Sharing engine - Multi APK Selection
    fun shareSelectedApps(context: Context) {
        val packagesToShare = selectedAppPackages.value
        if (packagesToShare.isEmpty()) {
            Toast.makeText(context, "Choose apps to share first", Toast.LENGTH_SHORT).show()
            return
        }

        val appsToExtract = _scannedApps.value.filter { packagesToShare.contains(it.packageName) }
        if (appsToExtract.isEmpty()) return

        viewModelScope.launch {
            _extractionProgress.value = 0.0f
            val extFiles = mutableListOf<File>()
            try {
                appsToExtract.forEachIndexed { index, app ->
                    _extractionMessage.value = "Extracting APK ${index + 1} of ${appsToExtract.size}:\n${app.name}"
                    val file = repository.extractApk(context, app) { localProgress ->
                        // Blend sub-progresses onto total timeline
                        val totalProgress = (index + localProgress) / appsToExtract.size
                        _extractionProgress.value = totalProgress
                    }
                    extFiles.add(file)
                    
                    // Add history items for audit/recently shared
                    repository.recordShare(app.packageName, app.name)
                }

                // Clear visual select states since files are packaged
                clearAppSelection()

                // Trigger Native Multi Share sheet
                triggerSystemShare(context, extFiles, "${appsToExtract.size} Applications")
            } catch (e: Exception) {
                Toast.makeText(context, "Batch Extraction failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _extractionProgress.value = null
                _extractionMessage.value = null
            }
        }
    }

    // Invokes native intent engine
    private fun triggerSystemShare(context: Context, files: List<File>, titlePrefix: String) {
        try {
            val authority = "com.d_Flor.package.fileprovider"
            val uris = files.map { FileProvider.getUriForFile(context, authority, it) }

            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "application/vnd.android.package-archive"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            val chooser = Intent.createChooser(intent, "Share $titlePrefix via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open sharing panel: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

class ShareViewModelFactory(
    private val application: Application,
    private val repository: SharingRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShareViewModel::class.java)) {
            return ShareViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
