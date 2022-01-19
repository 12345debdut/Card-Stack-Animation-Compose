package com.example.testapplication.views

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * It's for remembering the dragManager object after recomposition
 */
@Composable
fun rememberDragManager(size: Int, screenWidth: Float, scope: CoroutineScope, maxElements: Int) =
    remember {
        DragManager(
            size = size,
            screenWidth = screenWidth,
            scope = scope,
            maxElements = maxElements
        )
    }

open class DragState(
    val index: Int,
    val screenWidth: Float,
    private val scope: CoroutineScope
) {

    var opacity = Animatable(1f)
        private set

    var offsetX = Animatable(0f)
        private set
    var offsetY = Animatable(0f)
        private set
    var scale = Animatable(0f)
        private set

    init {

    }

    suspend fun drag(dragAmountX: Float, dragAmountY: Float) {
        offsetX.snapTo(dragAmountX)
    }

    fun positionToCenter(onParallel: suspend () -> Unit = {}) = scope.launch {
        launch { offsetX.animateTo(0f) }
        launch { offsetY.animateTo(0f) }
        launch { onParallel() }
    }

    fun animateOutsideOfScreen(onParallel: suspend () -> Unit = {}) = scope.launch {
        launch {
            offsetX.animateTo(-screenWidth)
        }
        launch {
            onParallel()
        }
    }

    suspend fun snap(scaleP: Float, opacityP: Float, offsetXP: Float) = scope.launch {
        launch { scale.snapTo(scaleP) }
        launch { opacity.snapTo(opacityP) }
        launch { offsetX.snapTo(offsetXP) }
    }

    suspend fun animateTo(
        scaleP: Float,
        opacityP: Float,
        offsetXP: Float,
        onParallel: suspend () -> Unit = {}
    ) = scope.launch {
        launch { scale.animateTo(scaleP) }
        launch { opacity.animateTo(opacityP) }
        launch { offsetX.animateTo(offsetXP) }
        launch { onParallel() }
    }

}