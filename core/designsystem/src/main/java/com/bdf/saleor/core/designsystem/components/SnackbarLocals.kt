package com.bdf.saleor.core.designsystem.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState is not provided")
}

val LocalTabReselectTick = staticCompositionLocalOf { 0 }
