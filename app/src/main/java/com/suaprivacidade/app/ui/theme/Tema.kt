package com.suaprivacidade.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suaprivacidade.app.data.Sensor
import com.suaprivacidade.app.data.Tema

/** Cor principal de cada tema; todo o esquema claro/escuro é derivado dela. */
val Tema.semente: Color
    get() = when (this) {
        Tema.SISTEMA -> Color(0xFF56657E)
        Tema.LAVANDA -> Color(0xFF7A65C7)
        Tema.MENTA -> Color(0xFF2C8A6D)
        Tema.PESSEGO -> Color(0xFFC25A3F)
        Tema.OCEANO -> Color(0xFF2A78B3)
        Tema.ROSE -> Color(0xFFB94D74)
    }

val Sensor.cor: Color
    get() = when (this) {
        Sensor.MICROFONE -> Color(0xFF8E6BD6)
        Sensor.CAMERA -> Color(0xFF2A9BC4)
    }

private val Branco = Color.White
private val Preto = Color.Black

private fun esquemaClaro(s: Color): ColorScheme = lightColorScheme(
    primary = s,
    onPrimary = Branco,
    inversePrimary = lerp(s, Branco, 0.55f),
    primaryContainer = lerp(s, Branco, 0.78f),
    onPrimaryContainer = lerp(s, Preto, 0.6f),
    secondary = lerp(s, Color(0xFF6B6B70), 0.45f),
    onSecondary = Branco,
    secondaryContainer = lerp(s, Branco, 0.84f),
    onSecondaryContainer = lerp(s, Preto, 0.65f),
    tertiary = lerp(s, Color(0xFF6B6B70), 0.3f),
    background = lerp(s, Branco, 0.965f),
    onBackground = lerp(s, Preto, 0.86f),
    surface = lerp(s, Branco, 0.965f),
    onSurface = lerp(s, Preto, 0.86f),
    surfaceVariant = lerp(s, Branco, 0.88f),
    onSurfaceVariant = lerp(s, Color(0xFF45454A), 0.82f),
    surfaceTint = s,
    surfaceContainerLowest = Branco,
    surfaceContainerLow = lerp(s, Branco, 0.94f),
    surfaceContainer = lerp(s, Branco, 0.92f),
    surfaceContainerHigh = lerp(s, Branco, 0.895f),
    surfaceContainerHighest = lerp(s, Branco, 0.87f),
    outline = lerp(s, Color(0xFF79797E), 0.75f),
    outlineVariant = lerp(s, Branco, 0.8f),
    error = Color(0xFFC0392B),
    onError = Branco,
    errorContainer = Color(0xFFFCE4E1),
    onErrorContainer = Color(0xFF5F1712),
)

private fun esquemaEscuro(s: Color): ColorScheme = darkColorScheme(
    primary = lerp(s, Branco, 0.42f),
    onPrimary = lerp(s, Preto, 0.72f),
    inversePrimary = s,
    primaryContainer = lerp(s, Preto, 0.5f),
    onPrimaryContainer = lerp(s, Branco, 0.82f),
    secondary = lerp(lerp(s, Color(0xFF8E8E94), 0.45f), Branco, 0.35f),
    onSecondary = lerp(s, Preto, 0.75f),
    secondaryContainer = lerp(s, Preto, 0.66f),
    onSecondaryContainer = lerp(s, Branco, 0.85f),
    tertiary = lerp(lerp(s, Color(0xFF8E8E94), 0.3f), Branco, 0.35f),
    background = lerp(s, Preto, 0.9f),
    onBackground = lerp(s, Branco, 0.9f),
    surface = lerp(s, Preto, 0.9f),
    onSurface = lerp(s, Branco, 0.9f),
    surfaceVariant = lerp(s, Preto, 0.72f),
    onSurfaceVariant = lerp(s, Branco, 0.7f),
    surfaceTint = lerp(s, Branco, 0.42f),
    surfaceContainerLowest = lerp(s, Preto, 0.94f),
    surfaceContainerLow = lerp(s, Preto, 0.82f),
    surfaceContainer = lerp(s, Preto, 0.79f),
    surfaceContainerHigh = lerp(s, Preto, 0.76f),
    surfaceContainerHighest = lerp(s, Preto, 0.72f),
    outline = lerp(s, Color(0xFF8E8E94), 0.7f),
    outlineVariant = lerp(s, Preto, 0.62f),
    error = Color(0xFFFF8A7D),
    onError = Color(0xFF5F1712),
    errorContainer = Color(0xFF5C1D17),
    onErrorContainer = Color(0xFFFFDAD5),
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val Tipografia = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun SuaPrivacidadeTheme(tema: Tema, content: @Composable () -> Unit) {
    val escuro = isSystemInDarkTheme()
    val esquema = remember(tema, escuro) { if (escuro) esquemaEscuro(tema.semente) else esquemaClaro(tema.semente) }
    MaterialTheme(colorScheme = esquema, typography = Tipografia, shapes = Formas, content = content)
}

val ColorScheme.escuro: Boolean get() = background.luminance() < 0.5f

/** Cor de "atenção" (uso silenciado, permissão nova), legível tanto no claro quanto no escuro. */
val ColorScheme.aviso: Color get() = if (escuro) Color(0xFFFFC069) else Color(0xFFA35F00)

/** Verde de "tudo certo", legível tanto no claro quanto no escuro. */
val ColorScheme.sucesso: Color get() = if (escuro) Color(0xFF6FD69B) else Color(0xFF23934F)

/** Ajusta a cor do sensor para ter bom contraste no tema atual. */
fun ColorScheme.corDoSensor(sensor: Sensor): Color =
    if (escuro) lerp(sensor.cor, Branco, 0.25f) else lerp(sensor.cor, Preto, 0.1f)
