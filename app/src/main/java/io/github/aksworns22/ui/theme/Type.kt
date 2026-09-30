package io.github.aksworns22.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.aksworns22.R

private val Paperlogy =
    FontFamily(
        Font(R.font.paperlogy_1thin, FontWeight.Thin),
        Font(R.font.paperlogy_2extralight, FontWeight.ExtraLight),
        Font(R.font.paperlogy_3light, FontWeight.Light),
        Font(R.font.paperlogy_4regular, FontWeight.Normal),
        Font(R.font.paperlogy_5medium, FontWeight.Medium),
        Font(R.font.paperlogy_6semibold, FontWeight.SemiBold),
        Font(R.font.paperlogy_7bold, FontWeight.Bold),
        Font(R.font.paperlogy_8extrabold, FontWeight.ExtraBold),
        Font(R.font.paperlogy_9black, FontWeight.Black)
    )

private fun TextStyle.paperlogy() = copy(fontFamily = Paperlogy)

private val base = Typography()

val Typography =
    Typography(
        displayLarge = base.displayLarge.paperlogy(),
        displayMedium = base.displayMedium.paperlogy(),
        displaySmall = base.displaySmall.paperlogy(),
        headlineLarge = base.headlineLarge.paperlogy(),
        headlineMedium = base.headlineMedium.paperlogy(),
        headlineSmall = base.headlineSmall.paperlogy(),
        titleLarge = base.titleLarge.paperlogy(),
        titleMedium = base.titleMedium.paperlogy(),
        titleSmall = base.titleSmall.paperlogy(),
        bodyLarge = base.bodyLarge.paperlogy(),
        bodyMedium = base.bodyMedium.paperlogy(),
        bodySmall = base.bodySmall.paperlogy(),
        labelLarge = base.labelLarge.paperlogy(),
        labelMedium = base.labelMedium.paperlogy(),
        labelSmall = base.labelSmall.paperlogy(),
        displayLargeEmphasized = base.displayLargeEmphasized.paperlogy(),
        displayMediumEmphasized = base.displayMediumEmphasized.paperlogy(),
        displaySmallEmphasized = base.displaySmallEmphasized.paperlogy(),
        headlineLargeEmphasized = base.headlineLargeEmphasized.paperlogy(),
        headlineMediumEmphasized = base.headlineMediumEmphasized.paperlogy(),
        headlineSmallEmphasized = base.headlineSmallEmphasized.paperlogy(),
        titleLargeEmphasized = base.titleLargeEmphasized.paperlogy(),
        titleMediumEmphasized = base.titleMediumEmphasized.paperlogy(),
        titleSmallEmphasized = base.titleSmallEmphasized.paperlogy(),
        bodyLargeEmphasized = base.bodyLargeEmphasized.paperlogy(),
        bodyMediumEmphasized = base.bodyMediumEmphasized.paperlogy(),
        bodySmallEmphasized = base.bodySmallEmphasized.paperlogy(),
        labelLargeEmphasized = base.labelLargeEmphasized.paperlogy(),
        labelMediumEmphasized = base.labelMediumEmphasized.paperlogy(),
        labelSmallEmphasized = base.labelSmallEmphasized.paperlogy()
    )
