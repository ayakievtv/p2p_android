package com.example.p2pandroidp2pandroid.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ColorScheme



private val DarkColorScheme = darkColorScheme(
    primary = Purple200,
    onPrimary = Color.Black,
    primaryContainer = Purple700,
    onPrimaryContainer = Purple200,
    secondary = Teal200,
    onSecondary = Color.Black,
    secondaryContainer = Teal700,
    tertiary = Pink200,
    onTertiary = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Purple500,
    onPrimary = Color.White,
    primaryContainer = Purple200,
    onPrimaryContainer = Color.Black,
    secondary = Teal500,
    onSecondary = Color.White,
    secondaryContainer = Teal200,
    tertiary = Pink500,
    onTertiary = Color.Black
)

@Composable
fun P2PAndroidTheme(
    colorScheme: ColorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
