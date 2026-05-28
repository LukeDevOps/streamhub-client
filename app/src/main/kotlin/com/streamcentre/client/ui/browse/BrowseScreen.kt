package com.streamcentre.client.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.Card
import coil.compose.AsyncImage
import com.streamcentre.client.api.ApiClient
import com.streamcentre.client.api.HistoryItem
import com.streamcentre.client.api.RecommendationItem
import com.streamcentre.client.app

@Composable
fun BrowseScreen(
    onSearchClick: () -> Unit,
    onItemSelected: (title: String) -> Unit,
    api: ApiClient = LocalContext.current.app.api,
    vm: BrowseViewModel = viewModel(factory = BrowseViewModel.factory(api)),
) {
    val recommendations by vm.recommendations.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        searchFocusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 48.dp, top = 32.dp, end = 48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Streamcentre",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Button(
                onClick = onSearchClick,
                modifier = Modifier.focusRequester(searchFocusRequester),
            ) {
                Text("Search")
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
                        items = history.map { it.displayTitle to it.tmdbId.toString() },
                        type = "show",
                        onSelect = { title -> onItemSelected(title) },
                    )
                    Spacer(Modifier.height(32.dp))
                }

                if (recommendations.isNotEmpty()) {
                    ContentRow(
                        title = "Recommended",
                        items = recommendations.map { it.title to it.ids.tmdb.toString() },
                        type = "movie",
                        onSelect = { title -> onItemSelected(title) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentRow(
    title: String,
    items: List<Pair<String, String>>,
    type: String,
    onSelect: (String) -> Unit,
) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 12.dp),
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(end = 16.dp),
    ) {
        items(items) { (itemTitle, tmdbId) ->
            PosterCard(
                title = itemTitle,
                tmdbId = tmdbId.toIntOrNull() ?: 0,
                type = type,
                onClick = { onSelect(itemTitle) },
            )
        }
    }
}

@Composable
private fun PosterCard(
    title: String,
    tmdbId: Int,
    type: String,
    onClick: () -> Unit,
) {
    val api = LocalContext.current.app.api
    var posterUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tmdbId) {
        if (tmdbId > 0) {
            runCatching { posterUrl = api.getPosterUrl(tmdbId, type) }
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(210.dp),
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
        }
    }
}
