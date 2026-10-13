package com.gabi.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta "Céu de Outono" — dourado/teal sobre azul-noite, em torno da
// personagem real da Gabi (ver mockup aprovado). Nomes antigos (Cyan/Orange/
// Blue) mantidos porque são referenciados em vários arquivos — só os valores
// mudaram, pra não precisar renomear em todo o app.
val CyanAccent = Color(0xFFFFCF8C)
val OrangeAccent = Color(0xFFD9A35C)
val BlueAccent = Color(0xFF5FB8AC)
val DarkBg = Color(0xFF0A0F16)
val SurfaceDark = Color(0xFF141D27)
val SurfaceMid = Color(0xFF1A2530)
val TextPrimary = Color(0xFFEAF6F5)
val TextSecondary = Color(0xFF8EA7AC)
val ErrorRed = Color(0xFFFF4D6A)
val SuccessGreen = Color(0xFF00E5B0)
/** Verde de marca do Spotify — usado só na caixinha de som reativa do Quarto da Gabi,
 *  pra ficar claramente reconhecível como "isso é o Spotify tocando" à primeira vista. */
val SpotifyGreen = Color(0xFF1ED760)

private val JavisColorScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = BlueAccent,
    tertiary = SuccessGreen,
    background = DarkBg,
    surface = SurfaceDark,
    surfaceVariant = SurfaceMid,
    onPrimary = DarkBg,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed
)

@Composable
fun JavisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JavisColorScheme,
        typography = JavisTypography,
        content = content
    )
}
