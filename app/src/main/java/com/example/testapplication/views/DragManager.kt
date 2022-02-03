package com.example.testapplication.views

import android.util.Log
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import com.google.android.material.math.MathUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

open class DragManager(
    val size: Int,
    private val screenWidth: Float,
    private val scope: CoroutineScope,
    private val maxElements: Int,
    private val animationSpec: AnimationSpec<Float> = spring()
) {
    private enum class DragDirection {
        LEFT,
        RIGHT
    }

    var listOfDragState: List<DragState> = listOf()
        private set

    private val maxCards = maxElements

    private var scale: List<Float> = listOf()

    private var opacity: List<Float> = listOf()

    private var offsetX: List<Float> = listOf()

    private var boxWidth: Float = 0f

    private var isAnimationRunning = false

    private var currentlyDragging = -1

    var topDeckIndex = mutableStateOf(size - 1)

    private var selectedIndex = size - 1
        set(value) {
            val temp = value.coerceIn(-1, size - 1)
            topDeckIndex.value = temp
            dragIndex = temp
            field = temp
        }

    private var dragIndex = -1


    /**
     * When the object initialize the object
     * */
    init {
        //Initializing drag states for all the cards
        initList()
        initProperty()
        initView()
    }

    /**
     * It will set the box width
     * */
    fun setBoxWidth(boxWidth: Float) {
        this.boxWidth = boxWidth
        initProperty()
        initView()
    }

    /**
     * It will just initialize the views with the exact dimensions
     */
    private fun initView() {
        scope.launch {
            listOfDragState.asReversed().mapIndexed { index, dragState ->
                when {
                    index >= maxCards -> {
                        return@mapIndexed
                    }
                    else -> dragState.snap(
                        scaleP = scale[index],
                        opacityP = opacity[index],
                        offsetXP = offsetX[index]
                    )
                }
            }
        }
    }

    /**
     * Initializing the helper properties to manipulate
     */
    private fun initProperty() {
        val scale = mutableListOf<Float>()
        val opacity = mutableListOf<Float>()
        val offsetX = mutableListOf<Float>()
        val scaleGap = CARD_STACK_SCALE_FACTOR
        val opacityGap = CARD_STACK_OPACITY_GAP
        val offsetGap = scaleGap * boxWidth
        for (i in 0 until maxCards) {
            scale.add(1f - i * scaleGap)
            opacity.add(i * opacityGap)
            offsetX.add(i * offsetGap)
        }
        this.scale = scale
        this.opacity = opacity
        this.offsetX = offsetX
    }

    /***
     * Initializing the list of drag state
     */
    private fun initList() {
        listOfDragState =
            List(size = size) {
                DragState(
                    index = it,
                    screenWidth = screenWidth,
                    scope = scope,
                    animationSpec = animationSpec
                )
            }
    }

    /**
     * It's responsible for performing the drag of the card
     * @param dragAmountX X axis drag amount
     * @param dragAmountY Y axis drag amount
     * @param dragIndex dragging index by the user
     * @param selectedIndex which one is currently top of the deck
     */
    private fun performDrag(
        dragAmountX: Float,
        dragAmountY: Float,
        dragIndex: Int,
        selectedIndex: Int,
        dragDirection: DragDirection = DragDirection.LEFT
    ) = scope.launch {
        if (dragIndex != selectedIndex) return@launch
        if (dragAmountX > 0) {
            return@launch
        }
        Log.d("INDEX", "DRAG INDEX: $dragIndex $dragAmountX")

        if (isAnimationRunning) return@launch
        if (currentlyDragging != -1 && currentlyDragging != dragIndex) return@launch
        val dragFraction = abs(dragAmountX).div(screenWidth / 2).coerceIn(0f, 1f)
        val dragFractionWRTScreen = abs(dragAmountX).div(screenWidth).coerceIn(0f, 1f)
        launch {
            //Only the top item should be removed from deck otherwise it will not respond
            val dragState = listOfDragState[dragIndex]
            //Linear interpolating the card itself opacity
            //if dragging the card towards left side then it will be fade out
            val opacity = MathUtils.lerp(0f,1f,dragFractionWRTScreen)
            dragState.snap(opacityP = opacity, offsetXP = dragAmountX)
        }
        for ((counter, i) in (dragIndex - 1 downTo (dragIndex - maxCards + 1).coerceAtLeast(0)).withIndex()) {
            val startIndex = counter + 1
            val endIndex = counter
            val scaleP =
                MathUtils.lerp(
                    scale[startIndex],
                    scale[endIndex],
                    abs(dragFraction)
                ) //[1f,0.8f,0.6f]
            val opacityP =
                MathUtils.lerp(
                    opacity[startIndex],
                    opacity[endIndex],
                    abs(dragFraction)
                ) //[0f,0.2f,0.4f]
            val offsetXP =
                MathUtils.lerp(offsetX[startIndex], offsetX[endIndex], abs(dragFraction)) //[]
            launch {
                listOfDragState[i].snap(
                    scaleP = scaleP,
                    opacityP = opacityP,
                    offsetXP = offsetXP
                )
            }
        }
    }

    /**
     * When deck of card ends dragging then need to handle couple of things
     * @param index which card user wants to swipe
     * @param selectedIndex currently which card is in the top of the deck
     */
    fun onDragEnd(index: Int, selectedIndex: Int) {
        if (index != selectedIndex) return
        val swipeState = listOfDragState[index]
        Log.d("INDEX", "SWIPE: $dragIndex")

        when {
            swipeState.offsetX.targetValue >= 0 -> {
                val prevIndex = (index + 1).coerceAtMost(size - 1)
                if (prevIndex == index + 1) {
                    isAnimationRunning = true
                    val prevState = listOfDragState[prevIndex]
                    prevState
                        .positionToCenter {
                            returnToEquilibrium(index = prevIndex)
                        }.invokeOnCompletion {
                            isAnimationRunning = false
                            this.selectedIndex -= -1
                        }
                }
            }
            abs(swipeState.offsetX.targetValue) < boxWidth / 2 -> {
                isAnimationRunning = true
                swipeState
                    .positionToCenter() {
                        returnToEquilibrium(index = index)
                    }.invokeOnCompletion {
                        isAnimationRunning = false
                    }
            }
            abs(swipeState.offsetX.targetValue) > 0 && abs(swipeState.offsetX.targetValue) > boxWidth / 2 -> {
                animateOutsideOfScreen(index = index).invokeOnCompletion {
                    this.selectedIndex -= 1
                }
            }
        }
    }

    /**
     * It's responsibility to returning the deck of cards to the equilibrium state in which state it belongs to
     * @param index Index of currently swiped card
     */
    private suspend fun returnToEquilibrium(index: Int) = scope.launch {
        launch {
            val dragState = listOfDragState[index]
            dragState.animateTo(opacityP = 0f)
        }
        for ((counter, i) in (index - 1 downTo (index - maxCards + 1).coerceAtLeast(0)).withIndex()) {
            listOfDragState[i].animateTo(
                scaleP = scale[counter + 1],
                opacityP = opacity[counter + 1],
                offsetXP = offsetX[counter + 1]
            )
        }
    }

    /**
     * It's the drag right gesture whenever user drags it right of the screen so that the last removed card should appear into the deck again
     * @param dragAmount Drag offset so that it will do the interpolation
     */
    fun dragRight(dragAmount: Offset) = scope.launch {
        if (dragAmount.x < 0) return@launch
        Log.d("DRAG RIGHT", "DRAG RIGHT: $selectedIndex")
        val prevItemIndex = (selectedIndex + 1).coerceAtMost(size - 1)
        if (prevItemIndex == selectedIndex + 1) {
            dragIndex = prevItemIndex
            Log.d("DRAG RIGHT", "DRAG RIGHT: here $selectedIndex")
            val item = listOfDragState[prevItemIndex]
            val itemOffset = Offset(x = item.offsetX.value, y = item.offsetY.value)
            val summed = itemOffset + dragAmount
            performDrag(
                dragAmountX = summed.x,
                dragAmountY = 0f,
                dragIndex = prevItemIndex,
                selectedIndex = prevItemIndex,
                dragDirection = DragDirection.RIGHT
            )
        }
    }

    /**
     * It's the drag right gesture whenever user drags it left of the screen so that the last removed card should appear into the deck again
     * @param dragAmount Drag offset so that it will do the interpolation
     */
    fun dragLeft(dragAmount: Offset) = scope.launch {
        if (dragAmount.x > 0) return@launch
        if (dragIndex != selectedIndex && dragIndex != -1) return@launch
        val item = listOfDragState[selectedIndex]
        val itemOffset = Offset(x = item.offsetX.value, y = item.offsetY.value)
        val summed = itemOffset + dragAmount
        performDrag(
            dragIndex = selectedIndex,
            dragAmountY = summed.y,
            dragAmountX = summed.x,
            selectedIndex = selectedIndex
        )
    }

    /**
     * It will give you the power to swiping left without interacting to the cards
     */
    fun swipeLeft() = scope.launch {
        animateOutsideOfScreen(index = selectedIndex)
            .invokeOnCompletion {
                selectedIndex -= 1
            }
    }

    /**
     * It will just help to swipe back to the deck
     */
    suspend fun swipeBack() = scope.launch {
        for ((counter, i) in (selectedIndex + 1 downTo (selectedIndex - maxCards + 1).coerceAtLeast(
            0
        )).withIndex()) {
            if (counter < maxCards && i < size) {
                val state = listOfDragState[i]
                state.animateTo(
                    scaleP = scale[counter],
                    opacityP = opacity[counter],
                    offsetXP = offsetX[counter]
                )
            }
        }
    }.invokeOnCompletion {
        selectedIndex -= -1
    }

    /**
     * It will animate outside of screen with the particular index
     * @param index Index of the current swiped card
     */
    private fun animateOutsideOfScreen(index: Int) = scope.launch {
        val state = listOfDragState[index]
        state.animateOutsideOfScreen()
        isAnimationRunning = true
        for ((counter, i) in (index - 1 downTo (index - maxCards + 1).coerceAtLeast(0)).withIndex()) {
            launch {
                listOfDragState[i].animateTo(
                    scaleP = scale[counter],
                    opacityP = opacity[counter],
                    offsetXP = offsetX[counter]
                ) {
                    if (index - maxCards >= 0) {
                        val lastElement = listOfDragState[index - maxCards]
                        lastElement.animateTo(
                            scaleP = scale[maxCards - 1],
                            opacityP = opacity[maxCards - 1],
                            offsetXP = offsetX[maxCards - 1]
                        )
                    }
                }
            }.invokeOnCompletion {
                isAnimationRunning = false
            }
        }
    }
}