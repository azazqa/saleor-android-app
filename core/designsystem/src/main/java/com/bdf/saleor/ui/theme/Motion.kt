package com.bdf.saleor.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

object AppMotion {
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
    val Standard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    const val IconFadeOut = 150
    const val IconFadeIn = 280
    const val LabelFade = 200
    const val IconScalePeak = 1.18f
}
