package com.example.ui

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sourceDir: String,
    val apkSize: Long,
    val isSystemApp: Boolean,
    val category: Int, // ApplicationInfo.category (e.g. CATEGORY_AUDIO, CATEGORY_GAME, etc.)
    val isFavorite: Boolean = false
) {
    // Helper to format bytes into elegant metrics (e.g. MB, GB, KB)
    val formattedSize: String
        get() {
            val kb = apkSize / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 1.0 -> String.format("%.2f MB", mb)
                kb >= 1.0 -> String.format("%.2f KB", kb)
                else -> "$apkSize B"
            }
        }
}
