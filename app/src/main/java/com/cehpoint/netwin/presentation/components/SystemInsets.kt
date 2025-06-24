package com.cehpoint.netwin.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun rememberWindowInsets(): WindowInsetsCompat {
    return androidx.compose.ui.platform.LocalView.current.let { view ->
        androidx.core.view.ViewCompat.getRootWindowInsets(view) ?: WindowInsetsCompat.CONSUMED
    }
}

@Composable
fun Modifier.systemBarsPadding(): Modifier = this.then(
    Modifier.padding(
        top = with(LocalDensity.current) { rememberWindowInsets().getInsets(WindowInsetsCompat.Type.statusBars()).top.toDp() },
        bottom = with(LocalDensity.current) { rememberWindowInsets().getInsets(WindowInsetsCompat.Type.navigationBars()).bottom.toDp() }
    )
)

@Composable
fun Modifier.statusBarPadding(): Modifier = this.then(
    Modifier.padding(
        top = with(LocalDensity.current) { rememberWindowInsets().getInsets(WindowInsetsCompat.Type.statusBars()).top.toDp() }
    )
)

@Composable
fun Modifier.navigationBarPadding(): Modifier = this.then(
    Modifier.padding(
        bottom = with(LocalDensity.current) { rememberWindowInsets().getInsets(WindowInsetsCompat.Type.navigationBars()).bottom.toDp() }
    )
) 