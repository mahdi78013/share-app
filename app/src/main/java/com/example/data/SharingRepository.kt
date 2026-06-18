package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.ui.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SharingRepository(private val sharingDao: SharingDao) {

    val favorites: Flow<List<FavoriteEntity>> = sharingDao.getFavorites()
    val sharingHistory: Flow<List<HistoryEntity>> = sharingDao.getSharingHistory()

    private var cachedScannedApps: List<AppInfo>? = null

    suspend fun addFavorite(packageName: String) {
        sharingDao.addFavorite(FavoriteEntity(packageName))
    }

    suspend fun removeFavorite(packageName: String) {
        sharingDao.removeFavoriteByPackage(packageName)
    }

    suspend fun recordShare(packageName: String, appName: String) {
        sharingDao.addHistoryEntry(HistoryEntity(packageName = packageName, appName = appName))
    }

    suspend fun clearHistory() {
        sharingDao.clearHistory()
    }

    // High performance background scanner of installed apps (shows only user-visible launcher/menu drawer apps)
    suspend fun scanInstalledApps(context: Context, forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedScannedApps != null) {
            return@withContext cachedScannedApps!!
        }

        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos = try {
            packageManager.queryIntentActivities(mainIntent, 0)
        } catch (e: Exception) {
            emptyList()
        }

        val result = resolveInfos.distinctBy { it.activityInfo.packageName }.map { resolveInfo ->
            async {
                val activityInfo = resolveInfo.activityInfo ?: return@async null
                val packageName = activityInfo.packageName
                
                val appInfo = activityInfo.applicationInfo ?: return@async null
                val name = resolveInfo.loadLabel(packageManager).toString()
                
                val packageInfo = try {
                    packageManager.getPackageInfo(packageName, 0)
                } catch (e: Exception) {
                    null
                } ?: return@async null

                val versionName = packageInfo.versionName ?: "Unknown"
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                val sourceDir = appInfo.sourceDir
                val apkFile = File(sourceDir)
                if (!apkFile.exists()) return@async null
                
                val apkSize = apkFile.length()
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val category = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    appInfo.category
                } else {
                    -1
                }
                
                AppInfo(
                    name = name,
                    packageName = packageName,
                    versionName = versionName,
                    versionCode = versionCode,
                    sourceDir = sourceDir,
                    apkSize = apkSize,
                    isSystemApp = isSystemApp,
                    category = category
                )
            }
        }.awaitAll().filterNotNull().sortedBy { it.name.lowercase() }

        cachedScannedApps = result
        result
    }

    // Background cleaning of stale extracted APK files older than 10 minutes
    fun cleanStaleApks(context: Context) {
        try {
            val outputDir = File(context.filesDir, "extracted_apks")
            if (outputDir.exists()) {
                outputDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".apk")) {
                        val age = System.currentTimeMillis() - file.lastModified()
                        if (age > 10 * 60 * 1000) {
                            file.delete()
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // High speed high fidelity chunked extraction engine
    suspend fun extractApk(context: Context, app: AppInfo, onProgress: (Float) -> Unit = {}): File = withContext(Dispatchers.IO) {
        val sourceFile = File(app.sourceDir)
        if (!sourceFile.exists()) {
            throw Exception("Source APK file not found at path: ${app.sourceDir}")
        }

        val outputDir = File(context.filesDir, "extracted_apks")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // Clean name (keep alphanumeric, language characters, digits, and underscores)
        val cleanName = app.name.replace("[^\\w\\p{L}]".toRegex(), "_").replace("_+".toRegex(), "_").trim('_')
        val displayName = if (cleanName.isNotBlank()) cleanName else "App"
        val outputFile = File(outputDir, "${displayName}_${app.packageName}.apk")

        // Chunked 64KB high speed transfer with progress logic
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var totalBytesWritten: Long = 0
                val totalLength = sourceFile.length()

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesWritten += bytesRead
                    if (totalLength > 0) {
                        onProgress(totalBytesWritten.toFloat() / totalLength)
                    }
                }
            }
        }
        outputFile
    }
}
