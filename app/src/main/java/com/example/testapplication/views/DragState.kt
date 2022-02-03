package com.example.testapplication.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * It's for remembering the dragManager object after recomposition
 */
@Composable
fun rememberDragManager(
    animationSpec: AnimationSpec<Float> = spring(),
    size: Int,
    maxCards: Int,
    scope: CoroutineScope
):DragManager {
    val config = LocalConfiguration.current
    val width = with(LocalDensity.current) { config.screenWidthDp.dp.toPx() }
    return remember {
        DragManager(
            animationSpec = animationSpec,
            size = size,
            maxElements = maxCards,
            screenWidth = width,
            scope = scope
        )
    }
}

open class DragState(
    val index: Int,
    val screenWidth: Float,
    private val scope: CoroutineScope,
    private val animationSpec: AnimationSpec<Float>
) {

    var opacity = Animatable(1f)
        private set

    var offsetX = Animatable(0f)
        private set
    var offsetY = Animatable(0f)
        private set
    var scale = Animatable(0f)
        private set

    private val dragFraction = mutableStateOf(0f)

    suspend fun drag(dragAmountX: Float, dragAmountY: Float,onParallel: suspend () -> Unit = {}) = scope.launch {
        dragFraction.value = abs(dragAmountX).div(screenWidth).coerceIn(0f, 1f)
        launch {
            offsetX.snapTo(dragAmountX)
        }
        launch {
            onParallel()
        }
    }

    fun positionToCenter(onParallel: suspend () -> Unit = {}) = scope.launch {
        launch { offsetX.animateTo(0f, animationSpec = animationSpec) }
        launch { offsetY.animateTo(0f, animationSpec = animationSpec) }
        launch { onParallel() }
    }

    fun animateOutsideOfScreen(onParallel: suspend () -> Unit = {}) = scope.launch {
        launch {
            offsetX.animateTo(-screenWidth, animationSpec = animationSpec)
        }
        launch {
            onParallel()
        }
    }

    suspend fun snap(scaleP: Float = 1f, opacityP: Float = 1f, offsetXP: Float = 0f) = scope.launch {
        launch { scale.snapTo(scaleP) }
        launch { opacity.snapTo(opacityP) }
        launch { offsetX.snapTo(offsetXP) }
    }

    suspend fun animateTo(
        scaleP: Float = 1f,
        opacityP: Float = 1f,
        offsetXP: Float = 0f,
        onParallel: suspend () -> Unit = {}
    ) = scope.launch {
        launch { scale.animateTo(scaleP, animationSpec = animationSpec) }
        launch { opacity.animateTo(opacityP, animationSpec = animationSpec) }
        launch { offsetX.animateTo(offsetXP, animationSpec = animationSpec) }
        launch { onParallel() }
    }

}