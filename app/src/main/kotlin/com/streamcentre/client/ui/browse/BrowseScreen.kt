package com.streamcentre.client.ui.browse

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import coil.compose.AsyncImage
import com.streamcentre.client.api.ApiClient
import com.streamcentre.client.app
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val FOCUS_SPRING = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

private data class BrowseItem(
    val title: String,
    val tmdbId: Int,
    val type: String,
    val progress: Float? = null,
)

@Composable
fun BrowseScreen(
    onSearchClick: () -> Unit,
    onItemSelected: (title: String) -> Unit,
    api: ApiClient = LocalContext.current.app.api,
    vm: BrowseViewModel = viewModel(factory = BrowseViewModel.factory(api)),
) {
    val recommendations by vm.recommendations.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val resumePositions by vm.resumePositions.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val searchFocusRequester = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    val searchScale by animateFloatAsState(
        targetValue = if (searchFocused) 1.06f else 1f,
        animationSpec = FOCUS_SPRING,
        label = "searchScale",
    )

    val dotX = remember { Animatable(0f) }
    val dotY = remember { Animatable(0f) }
    val dotAlpha = remember { Animatable(0f) }
    var dotInitialized by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    fun onFocused(getCoords: () -> LayoutCoordinates?, listState: LazyListState? = null) {
        scope.launch {
            launch { dotAlpha.animateTo(1f, tween(80)) }
            // Let column scroll start, then wait for it to finish.
            delay(16)
            snapshotFlow { scrollState.isScrollInProgress }.first { !it }
            // Let LazyRow scroll start (if any), then wait for it to finish.
            if (listState != null) {
                delay(16)
                snapshotFlow { listState.isScrollInProgress }.first { !it }
            }
            delay(16)
            val coords = getCoords() ?: return@launch
            val bounds = coords.boundsInRoot()
            val targetX = bounds.center.x
            val targetY = bounds.bottom + 15f * density.density
            if (!dotInitialized) {
                dotX.snapTo(targetX)
                dotY.snapTo(targetY)
                dotInitialized = true
            } else {
                launch { dotX.animateTo(targetX, FOCUS_SPRING) }
                launch { dotY.animateTo(targetY, FOCUS_SPRING) }
            }
        }
    }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(start = 48.dp, top = 32.dp, end = 48.dp, bottom = 48.dp),
        ) {
            var searchCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .graphicsLayer { scaleX = searchScale; scaleY = searchScale }
                        .border(1.dp, Color.White, RoundedCornerShape(50))
                        .focusRequester(searchFocusRequester)
                        .onGloballyPositioned { searchCoords = it }
                        .onFocusChanged { state ->
                            searchFocused = state.isFocused
                            if (state.isFocused) onFocused({ searchCoords })
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSearchClick,
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Search", color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(32.dp))

            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { vm.load() }) { Text("Retry") }
                        }
                    }
                }
                else -> {
                    if (history.isNotEmpty()) {
                        ContentRow(
                            title = "Continue Watching",
                            items = history.map { item ->
                                val pos = resumePositions[item.contentId]
                                val progress = if (pos != null && pos.duration > 0)
                                    (pos.position.toFloat() / pos.duration).coerceIn(0f, 1f)
                                else null
                                BrowseItem(item.displayTitle, item.tmdbId, item.mediaType, progress)
                            },
                            onSelect = { onItemSelected(it) },
                            onFocused = { getCoords, listState -> onFocused(getCoords, listState) },
                        )
                        Spacer(Modifier.height(32.dp))
                    }

                    if (recommendations.isNotEmpty()) {
                        ContentRow(
                            title = "Recommended",
                            items = recommendations.map { BrowseItem(it.title, it.ids.tmdb, "movie") },
                            onSelect = { onItemSelected(it) },
                            onFocused = { getCoords, listState -> onFocused(getCoords, listState) },
                        )
                    }
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = dotAlpha.value),
                radius = 5.dp.toPx(),
                center = Offset(dotX.value, dotY.value),
            )
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    items: List<BrowseItem>,
    onSelect: (String) -> Unit,
    onFocused: (getCoords: () -> LayoutCoordinates?, listState: LazyListState) -> Unit,
) {
    val listState = rememberLazyListState()
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 12.dp),
    )
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp, end = 16.dp),
    ) {
        items(items) { item ->
            PosterCard(
                title = item.title,
                tmdbId = item.tmdbId,
                type = item.type,
                progress = item.progress,
                onClick = { onSelect(item.title) },
                onFocused = { getCoords -> onFocused(getCoords, listState) },
            )
        }
    }
}

@Composable
private fun PosterCard(
    title: String,
    tmdbId: Int,
    type: String,
    progress: Float? = null,
    onClick: () -> Unit,
    onFocused: (() -> LayoutCoordinates?) -> Unit,
) {
    val api = LocalContext.current.app.api
    val posterUrl = if (tmdbId > 0) api.posterImageUrl(tmdbId, type) else null
    var cardCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = FOCUS_SPRING,
        label = "cardScale",
    )

    Card(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1f),
        modifier = Modifier
            .width(140.dp)
            .height(210.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onGloballyPositioned { cardCoords = it }
            .onFocusChanged { state ->
                focused = state.isFocused
                if (state.isFocused) onFocused { cardCoords }
            },
    ) {
        Box(Modifier.fillMaxSize()) {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title.take(2).uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp),
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 2,
                )
            }
            if (progress != null && progress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}
