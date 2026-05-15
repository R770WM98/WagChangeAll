package com.thisismine.myapplication.core.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class CardDensityMode {
    Compact,
    Comfortable
}

data class CardDensity(
    val listContentPadding: Dp,
    val sectionSpacing: Dp,
    val cardPadding: Dp,
    val rowSpacing: Dp
)

val LocalCardDensity = compositionLocalOf { CardDensityMode.Comfortable.toDensity() }

fun CardDensityMode.toDensity(): CardDensity = when (this) {
    CardDensityMode.Compact -> CardDensity(
        listContentPadding = 12.dp,
        sectionSpacing = 8.dp,
        cardPadding = 12.dp,
        rowSpacing = 6.dp
    )
    CardDensityMode.Comfortable -> CardDensity(
        listContentPadding = 16.dp,
        sectionSpacing = 12.dp,
        cardPadding = 16.dp,
        rowSpacing = 10.dp
    )
}

