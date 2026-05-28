package com.streamcentre.client.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.streamcentre.client.api.ApiClient
import com.streamcentre.client.api.SearchResult
import com.streamcentre.client.api.StreamResponse
import com.streamcentre.client.app

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SearchScreen(
    initialQuery: String = "",
    onPlay: (url: String, contentId: String, infoHash: String, duration: Float) -> Unit,
    api: ApiClient = LocalContext.current.app.api,
    vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(api)),
) {
    var query by remember { mutableStateOf(initialQuery) }
    var selectedCategory by remember { mutableIntStateOf(ApiClient.CATEGORY_MOVIES) }

    val results by vm.results.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val isStartingStream by vm.isStartingStream.collectAsStateWithLifecycle()
    val streamResult by vm.streamResult.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val searchFocusRequester = remember { FocusRequester() }

    // Pre-fill and search if launched from browse
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            query = initialQuery
            vm.search(initialQuery, selectedCategory)
        } else {
            searchFocusRequester.requestFocus()
        }
    }

    // Navigate to player once stream is ready
    LaunchedEffect(streamResult) {
        streamResult?.let { s ->
            onPlay(
                "${api.baseUrl}${s.url}",
                s.contentId,
                infoHashFromMagnet(s.magnetUrl),
                s.durationSeconds.toFloat(),
            )
            vm.clearStream()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
        ) {
            Text(
                text = "Search",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester),
                )
                FilterChip(
                    selected = selectedCategory == ApiClient.CATEGORY_MOVIES,
                    onClick = { selectedCategory = ApiClient.CATEGORY_MOVIES },
                    label = { Text("Movies") },
                )
                FilterChip(
                    selected = selectedCategory == ApiClient.CATEGORY_TV,
                    onClick = { selectedCategory = ApiClient.CATEGORY_TV },
                    label = { Text("TV") },
                )
                Button(
                    onClick = { vm.search(query, selectedCategory) },
                    enabled = query.isNotBlank() && !isSearching,
                ) {
                    Text("Search")
                }
            }

            Spacer(Modifier.height(24.dp))

            when {
                isSearching || isStartingStream -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (isStartingStream) "Starting stream…" else "Searching…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                results.isEmpty() && query.isNotBlank() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No results", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(results) { result ->
                            ResultCard(
                                result = result,
                                onClick = { vm.startStream(result) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ResultCard(result: SearchResult, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${result.quality}  ·  ${result.indexer}  ·  ${formatSize(result.size)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "▲ ${result.seeders}",
                fontSize = 13.sp,
                color = if (result.seeders > 5) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val gb = bytes / 1_073_741_824.0
    val mb = bytes / 1_048_576.0
    return if (gb >= 1) "%.1f GB".format(gb) else "%.0f MB".format(mb)
}

private fun infoHashFromMagnet(magnetUrl: String): String =
    magnetUrl
        .substringAfter("urn:btih:", "")
        .substringBefore("&")
        .lowercase()
