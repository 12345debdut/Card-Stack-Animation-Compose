package com.example.testapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.fragment.app.Fragment
import coil.compose.LocalImageLoader
import coil.compose.rememberImagePainter
import com.example.testapplication.views.LazyCardStack
import com.example.testapplication.views.LazyStackIndicator
import com.example.testapplication.views.rememberDragManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map


class FirstFragement : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first_fragement, container, false)
    }

    @ExperimentalMaterialApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            AutoScrollCardStack(list = mutableListOf(
                    R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3,
            R.drawable.image4,
            R.drawable.image5,
            R.drawable.image6
        ))
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AutoScrollCardStack(list: List<Int>){
    val scope = rememberCoroutineScope()
    val dragState = rememberDragManager(
        animationSpec = tween(durationMillis = 500),
        size = list.size,
        maxCards = 3,
        scope = scope
    )
    LaunchedEffect(Unit) {
        var swipingLeft = true
        snapshotFlow{
            dragState.topDeckIndex.value
        }.map {
            delay(2000)
            if(it == 0){
                swipingLeft = false
            }else if(it == list.size-1){
                swipingLeft = true
            }
            it
        }.collect {
            if(swipingLeft) {
                dragState.swipeLeft()
            }else{
                dragState.swipeBack()
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageLoader = LocalImageLoader.current
        LazyCardStack(
            items = list,
            maxElements = 3,
            content = {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape = RoundedCornerShape(percent = 10)),
                    painter = rememberImagePainter(data = it, imageLoader = imageLoader),
                    contentDescription = "",
                    contentScale = ContentScale.Crop
                )
            },
            modifier = Modifier
                .fillMaxSize(fraction = 0.9f),
            dragState = dragState,
            isDragEnable = false
        )
        LazyStackIndicator(
            dragState = dragState,
            count = list.size
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun UserDrivenCardStack(list:List<Int>){
    val scope = rememberCoroutineScope()
    val dragState = rememberDragManager(
        animationSpec = tween(durationMillis = 500),
        size = list.size,
        maxCards = 3,
        scope = scope
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val imageLoader = LocalImageLoader.current
        LazyCardStack(
            items = list,
            maxElements = 3,
            content = {
                Image(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape = RoundedCornerShape(percent = 10)),
                    painter = rememberImagePainter(data = it, imageLoader = imageLoader),
                    contentDescription = "",
                    contentScale = ContentScale.Crop
                )
            },
            modifier = Modifier
                .fillMaxSize(fraction = 0.9f),
            dragState = dragState,
            isDragEnable = true
        )
        LazyStackIndicator(
            dragState = dragState,
            count = list.size
        )
    }
}