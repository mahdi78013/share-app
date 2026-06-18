package com.example.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderSelected
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfaceSelected

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isSelected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) GlassSurfaceSelected else GlassSurface,
        animationSpec = tween(durationMillis = 250),
        label = "containerColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) GlassBorderSelected else GlassBorder,
        animationSpec = tween(durationMillis = 250),
        label = "borderColor"
    )

    Column(
        modifier = modifier
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(cornerRadius))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(containerColor),
        content = content
    )
}


private val iconCache = object : android.util.LruCache<String, ImageBitmap>(
    (Runtime.getRuntime().maxMemory() / 8).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
) {
    override fun sizeOf(key: String, value: ImageBitmap): Int {
        return value.width * value.height * 4 // size in bytes for ARGB_8888 bitmap
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmapState by remember(packageName) { 
        mutableStateOf(iconCache.get(packageName)) 
    }

    if (bitmapState == null) {
        LaunchedEffect(packageName) {
            withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val drawable = pm.getApplicationIcon(packageName)
                    val bitmap = drawable.toBitmap(
                        width = 96,
                        height = 96,
                        config = Bitmap.Config.ARGB_8888
                    ).asImageBitmap()
                    iconCache.put(packageName, bitmap)
                    bitmapState = bitmap
                } catch (e: Exception) {
                    try {
                        val defaultDrawable = context.packageManager.defaultActivityIcon
                        val bitmap = defaultDrawable.toBitmap(
                            width = 96,
                            height = 96,
                            config = Bitmap.Config.ARGB_8888
                        ).asImageBitmap()
                        bitmapState = bitmap
                    } catch (ex: Exception) {
                        bitmapState = null
                    }
                }
            }
        }
    }

    val bitmap = bitmapState
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = contentDescription,
                tint = ElectricCyan.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun GlassSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search icon",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Box(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Standard light weight input field that responds beautifully
            BasicTextFieldColors(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                }),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (value.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear Search",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable { onValueChange("") }
            )
        }
    }
}

// Minimalist wrapper for basic text input containing standard text field bindings
@Composable
private fun BasicTextFieldColors(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = Brush.verticalGradient(listOf(ElectricCyan, ElectricCyan)),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}
