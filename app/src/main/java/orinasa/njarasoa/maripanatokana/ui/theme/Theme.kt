package orinasa.njarasoa.maripanatokana.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppDarkPrimary,
    onPrimary = AppDarkOnPrimary,
    primaryContainer = AppDarkPrimaryContainer,
    onPrimaryContainer = AppDarkOnPrimaryContainer,
    secondary = AppDarkSecondary,
    onSecondary = AppDarkOnSecondary,
    secondaryContainer = AppDarkSecondaryContainer,
    onSecondaryContainer = AppDarkOnSecondaryContainer,
    background = AppDarkBackground,
    onBackground = AppDarkOnBackground,
    surface = AppDarkSurface,
    onSurface = AppDarkOnSurface,
    surfaceVariant = AppDarkSurfaceVariant,
    onSurfaceVariant = AppDarkOnSurfaceVariant,
    error = AppDarkError,
    onError = AppDarkOnError,
    errorContainer = AppDarkErrorContainer,
    onErrorContainer = AppDarkOnErrorContainer,
    outline = AppDarkOutline,
    outlineVariant = AppDarkOutlineVariant,
)

private val LightColorScheme = lightColorScheme(
    primary = AppDarkPrimaryContainer,
    onPrimary = AppDarkOnPrimaryContainer,
    background = AppDarkBackground,
    onBackground = AppDarkOnBackground,
    surface = AppDarkBackground,
    onSurface = AppDarkOnSurface,
)

@Composable
fun MaripanaTokanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
