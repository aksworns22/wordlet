package io.github.aksworns22.wordlet.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.aksworns22.wordlet.R

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

// 핵심 요소에 쓰는 Emphasized 스타일은 기본보다 두 단계 굵게 해 시선을 모은다.
private fun TextStyle.emphasized() =
    paperlogy().copy(
        fontWeight = FontWeight((fontWeight ?: FontWeight.Normal).weight.plus(200).coerceAtMost(900))
    )

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
        displayLargeEmphasized = base.displayLargeEmphasized.emphasized(),
        displayMediumEmphasized = base.displayMediumEmphasized.emphasized(),
        displaySmallEmphasized = base.displaySmallEmphasized.emphasized(),
        headlineLargeEmphasized = base.headlineLargeEmphasized.emphasized(),
        headlineMediumEmphasized = base.headlineMediumEmphasized.emphasized(),
        headlineSmallEmphasized = base.headlineSmallEmphasized.emphasized(),
        titleLargeEmphasized = base.titleLargeEmphasized.emphasized(),
        titleMediumEmphasized = base.titleMediumEmphasized.emphasized(),
        titleSmallEmphasized = base.titleSmallEmphasized.emphasized(),
        bodyLargeEmphasized = base.bodyLargeEmphasized.emphasized(),
        bodyMediumEmphasized = base.bodyMediumEmphasized.emphasized(),
        bodySmallEmphasized = base.bodySmallEmphasized.emphasized(),
        labelLargeEmphasized = base.labelLargeEmphasized.emphasized(),
        labelMediumEmphasized = base.labelMediumEmphasized.emphasized(),
        labelSmallEmphasized = base.labelSmallEmphasized.emphasized()
    )
