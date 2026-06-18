package com.example.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HistoryEntity
import com.example.ui.theme.*

@Composable
fun MainDashboard(
    viewModel: ShareViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe streams safely with lifecycle awareness
    val apps by viewModel.appsFlow.collectAsStateWithLifecycle()
    val historyLog by viewModel.sharingHistory.collectAsStateWithLifecycle()
    
    val selectedPackages by viewModel.selectedAppPackages.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val activeFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val timeUpdateTick by viewModel.triggerTimeUpdate.collectAsStateWithLifecycle()

    val extractionProgress by viewModel.extractionProgress.collectAsStateWithLifecycle()
    val extractionMessage by viewModel.extractionMessage.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        WelcomeSplashScreen(onDismiss = { 
            showSplash = false 
            viewModel.scanApps(context)
        })
    } else {
        // Heavy slate black canvas with glowing background orbs
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBg)
        ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CorporateBlue.copy(alpha = 0.28f), Color.Transparent),
                    radius = size.width * 0.7f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricCyan.copy(alpha = 0.20f), Color.Transparent),
                    radius = size.width * 0.6f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentTeal.copy(alpha = 0.15f), Color.Transparent),
                    radius = size.width * 0.45f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.45f)
            )
        }

        // Scaffold for safe edge-to-edge support while maintaining ambient glow backgrounds
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            bottomBar = {
                // Glow Floating Selection Bar for multi extraction
                AnimatedVisibility(
                    visible = selectedPackages.isNotEmpty() && activeTab != TabType.RECENT,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(200)
                    ) + fadeOut()
                ) {
                    val count = selectedPackages.size
                    // Compute selected apps bytes
                    val bytes = remember(selectedPackages, apps) {
                        apps.filter { selectedPackages.contains(it.packageName) }.sumOf { it.apkSize }
                    }
                    val formattedTotalSize = remember(bytes) {
                        val kb = bytes / 1024.0
                        val mb = kb / 1024.0
                        if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.1f KB", kb)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        GlassContainer(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            isSelected = true
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "$count App${if (count > 1) "s" else ""} Selected",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Size: $formattedTotalSize",
                                        color = LightSlate.copy(alpha = 0.8f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Elegant circular close button
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.08f))
                                            .border(1.dp, GlassBorder, CircleShape)
                                            .clickable { viewModel.clearAppSelection() }
                                            .testTag("clear_selection_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel selection",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Gradient Rich Multi-Share Action Trigger
                                    Box(
                                        modifier = Modifier
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(CorporateBlue, ElectricCyan)
                                                )
                                            )
                                            .clickable { viewModel.shareSelectedApps(context) }
                                            .padding(horizontal = 18.dp)
                                            .testTag("multi_share_submit_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Share APKs",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // Top Corporate Branding Header
                HeaderSection(
                    totalCount = apps.size,
                    onRefresh = { viewModel.scanApps(context) },
                    isRefreshActive = isScanning
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Search Field (not needed/visible on History Tab)
                if (activeTab != TabType.RECENT) {
                    GlassSearchField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = "Search apps or package...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("app_search_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Smooth Sliding Glass Tabs
                SegmentedTabs(
                    activeTab = activeTab,
                    onTabSelected = { viewModel.updateSelectedTab(it) }
                )

                if (activeTab != TabType.RECENT) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryFilterRow(
                        activeFilter = activeFilter,
                        onFilterChanged = { viewModel.updateSelectedFilter(it) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Core Grid List
                Box(modifier = Modifier.weight(1f)) {
                    if (isScanning && apps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ElectricCyan)
                        }
                    } else {
                        when (activeTab) {
                            TabType.ALL -> {
                                if (apps.isEmpty()) {
                                    EmptyStateView(
                                        title = "No Apps Found",
                                        subtitle = "No apps match your filter choices or search parameters.",
                                        icon = Icons.Outlined.Search
                                    )
                                } else {
                                    AppsList(
                                        apps = apps,
                                        selectedPackages = selectedPackages,
                                        onAppClick = { app ->
                                            viewModel.toggleAppSelection(app.packageName)
                                        },
                                        onShareClick = { app ->
                                            viewModel.shareSingleApp(context, app)
                                        }
                                    )
                                }
                            }
                            TabType.RECENT -> {
                                if (historyLog.isEmpty()) {
                                    EmptyStateView(
                                        title = "History Log Empty",
                                        subtitle = "All your recently extracted apps and sharing bundles will appear here.",
                                        icon = Icons.Outlined.History
                                    )
                                } else {
                                    HistoryView(
                                        historyList = historyLog,
                                        timeUpdateTick = timeUpdateTick,
                                        onClearAll = { viewModel.clearHistory() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overlay: Glassmorphic Loading Progress panel during extraction
        AnimatedVisibility(
            visible = extractionProgress != null,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            ExtractionOverlay(
                progress = extractionProgress,
                message = extractionMessage ?: "Extracting Application package APK..."
            )
        }
    }
    }
}

@Composable
fun HeaderSection(
    totalCount: Int,
    onRefresh: () -> Unit,
    isRefreshActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Share App",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Premium Extraction & Instant Sharing",
                color = LightSlate.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }

        // Quick Mini Analytics
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stats Badge - Symmetrical 36.6dp height
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$totalCount",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Refresh Button - Symmetrical 36.dp size & perfectly matching styling
            val rotation by animateFloatAsState(
                targetValue = if (isRefreshActive) 360f else 0f,
                animationSpec = if (isRefreshActive) {
                    infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart)
                } else {
                    snap()
                },
                label = "rotateRefresh"
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onRefresh() }
                    .testTag("refresh_installed_apps_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Search devices",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .scale(1.1f)
                        .rotateWithOffset(rotation)
                )
            }
        }
    }
}

// Simple floating modifier to support rotation offsets gracefully
private fun Modifier.rotateWithOffset(degrees: Float): Modifier = this.then(
    Modifier.graphicsLayer {
        rotationZ = degrees
    }
)

@Composable
fun CategoryFilterRow(
    activeFilter: FilterType,
    onFilterChanged: (FilterType) -> Unit
) {
    val filters = listOf(
        FilterType.ALL to "All Apps",
        FilterType.USER to "Downloaded",
        FilterType.SYSTEM to "System",
        FilterType.LARGE to "Large (>50MB)"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(filters) { (filter, label) ->
            val isActive = activeFilter == filter
            val pillBg by animateColorAsState(
                targetValue = if (isActive) CorporateBlue else GlassSurface,
                label = "pillBg"
            )
            val pillBorder by animateColorAsState(
                targetValue = if (isActive) GlassBorderSelected else GlassBorder,
                label = "pillBorder"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(pillBg)
                    .border(1.dp, pillBorder, RoundedCornerShape(20.dp))
                    .clickable { onFilterChanged(filter) }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    color = if (isActive) Color.White else LightSlate,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SegmentedTabs(
    activeTab: TabType,
    onTabSelected: (TabType) -> Unit
) {
    val tabs = listOf(
        TabType.ALL to "Installed",
        TabType.RECENT to "History Log"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = activeTab == tab
            
            val tabBg by animateColorAsState(
                targetValue = if (isSelected) GlassSurfaceSelected else Color.Transparent,
                label = "tabBg"
            )
            val tabBorder by animateColorAsState(
                targetValue = if (isSelected) GlassBorderSelected else Color.Transparent,
                label = "tabBorder"
            )
            val tabTextColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else SlateGray,
                label = "tabText"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(tabBg)
                    .border(1.dp, tabBorder, RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab) }
                    .testTag("tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = when(tab) {
                            TabType.ALL -> Icons.Default.Android
                            TabType.RECENT -> Icons.Default.History
                            else -> Icons.Default.Android
                        },
                        contentDescription = null,
                        tint = if (isSelected) ElectricCyan else tabTextColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        color = tabTextColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AppsList(
    apps: List<AppInfo>,
    selectedPackages: Set<String>,
    onAppClick: (AppInfo) -> Unit,
    onShareClick: (AppInfo) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(apps, key = { it.packageName }) { app ->
            val isSelected = selectedPackages.contains(app.packageName)
            
            GlassContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAppClick(app) }
                    .testTag("app_item_${app.packageName}"),
                isSelected = isSelected
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Multi-select Checkbox glowing accent
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) CorporateBlue else Color.White.copy(alpha = 0.05f))
                            .border(
                                1.dp,
                                if (isSelected) ElectricCyan else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "App Selected",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Embedded Safe Loader App Icon
                    AppIconImage(
                        packageName = app.packageName,
                        contentDescription = "${app.name} icon",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Text Details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "v${app.versionName} • ${app.formattedSize}",
                            color = LightSlate.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Action buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Ultra-Premium Polished Gradient Share Button
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(CorporateBlue, ElectricCyan)
                                    )
                                )
                                .clickable { onShareClick(app) }
                                .padding(horizontal = 14.dp)
                                .testTag("share_button_${app.packageName}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share now",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Share",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryView(
    historyList: List<HistoryEntity>,
    timeUpdateTick: Long,
    onClearAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sharing Audit Record",
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Clear History",
                color = GlassError,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClearAll() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("clear_history_log_button")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(historyList, key = { it.id }) { entry ->
                GlassContainer(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CorporateBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.appName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = entry.packageName,
                                color = LightSlate.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val formattedTime = remember(entry.sharedAt, timeUpdateTick) {
                            val diff = System.currentTimeMillis() - entry.sharedAt
                            when {
                                diff < 60_000 -> "Just now"
                                diff < 3600_000 -> "${diff / 60_000}m ago"
                                diff < 86400_000 -> "${diff / 3600_000}h ago"
                                else -> "${diff / 86400_000}d ago"
                            }
                        }

                        Text(
                            text = formattedTime,
                            color = LightSlate.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassContainer(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    color = LightSlate.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ExtractionOverlay(
    progress: Float?,
    message: String
) {
    // Frosted full-bleed background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f))
            .clickable(enabled = false) {}, // absorb clicks during extraction
        contentAlignment = Alignment.Center
    ) {
        GlassContainer(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp),
            cornerRadius = 24.dp,
            isSelected = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress ?: 0f },
                        modifier = Modifier.size(72.dp),
                        color = ElectricCyan,
                        strokeWidth = 5.dp,
                        trackColor = Color.White.copy(alpha = 0.15f),
                    )
                    
                    val pct = if (progress != null) "${(progress * 100).toInt()}%" else "..."
                    Text(
                        text = pct,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Extraction Active",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message,
                    color = LightSlate.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun CustomShareLogo(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "logoAnimation")
    
    // Smooth translation of flying packet blocks representing premium transfers
    val transitionOffset1 by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )
    val transitionOffset2 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Glowing ambient backdrop
        Canvas(modifier = Modifier.size(180.dp, 130.dp)) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(CorporateBlue.copy(alpha = 0.2f), Color.Transparent),
                    radius = size.width * 0.7f
                )
            )
        }

        // Structural layered graphics - non-circular complete composition
        Box(
            modifier = Modifier
                .size(140.dp, 100.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.03f))
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Underlay circuit lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                // Draw tech corporate alignment lines
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    start = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.5f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.5f),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = ElectricCyan.copy(alpha = 0.12f),
                    start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.2f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.8f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // High-end glass layers floating
            Row(
                horizontalArrangement = Arrangement.spacedBy((-18).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender Card (Left/Blue glass)
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = transitionOffset1
                        }
                        .size(46.dp, 58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, GlassBorderSelected.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Transfer Hub center node
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(CorporateBlue, ElectricCyan)))
                        .padding(1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(DarkBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Receiver Card (Right glass)
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = transitionOffset2
                        }
                        .size(46.dp, 58.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, GlassBorderSelected.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeSplashScreen(
    onDismiss: () -> Unit
) {
    // 2 Seconds delay then dismiss splash screen
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Glowing elegant corporate ambient flows (non-cyberpunk)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CorporateBlue.copy(alpha = 0.15f), Color.Transparent),
                    radius = size.width * 1.1f
                ),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            CustomShareLogo(
                modifier = Modifier.size(180.dp, 130.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Share App",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineLarge,
                letterSpacing = 1.0.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Premium Extraction & Instant Sharing",
                color = LightSlate.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            CircularProgressIndicator(
                color = ElectricCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}




