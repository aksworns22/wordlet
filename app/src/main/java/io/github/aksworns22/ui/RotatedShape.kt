package io.github.aksworns22.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * [shape]를 가운데를 축으로 [degrees]만큼 돌린 도형.
 *
 * API 30 미만에서는 오목한 도형으로 clip한 레이어를 rotationZ로 돌리면 도형이 직선으로 잘려 보인다.
 * 레이어 대신 도형 자체를 돌리면 어느 버전에서나 온전히 돈다.
 * 돌린 모서리가 레이아웃 경계를 벗어날 수 있으니 채우기는 clip 대신 `background(color, shape)`로 그린다.
 */
class RotatedShape(
    private val shape: Shape,
    private val degrees: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val outline = shape.createOutline(size, layoutDirection, density)
        if (degrees % 360f == 0f) return outline
        val path = Path().apply { addOutline(outline) }
        val cx = size.width / 2
        val cy = size.height / 2
        path.transform(
            Matrix().apply {
                translate(cx, cy)
                rotateZ(degrees)
                translate(-cx, -cy)
            }
        )
        return Outline.Generic(path)
    }
}
