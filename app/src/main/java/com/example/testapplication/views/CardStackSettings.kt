package com.example.testapplication.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

const val CARD_STACK_SCALE_FACTOR = 0.15f
const val CARD_STACK_OPACITY_GAP = 0.4f

object CardStackDefaults {
    val CornerShape = RoundedCornerShape(percent = 10)
    val Elevation = 10.dp
    val HorizontalAlignment = Alignment.CenterHorizontally
    val VerticalArrangement = Arrangement.Center
}
