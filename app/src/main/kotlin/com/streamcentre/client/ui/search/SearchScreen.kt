package com.streamcentre.client.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import com.streamcentre.client.api.ApiClient
import com.streamcentre.client.api.SearchResult
import com.streamcentre.client.app
import kotlinx.coroutines.delay

@Composable
private fun CategoryToggle(selected: Int, onSelect: (Int) -> Unit) {
    val chipColors = SegmentedButtonDefaults.colors(
        activeContainerColor = ORANGE,
        activeContentColor = Color.White,
        activeBorderColor = Color.Transparent,
        inactiveContainerColor = Color.White.copy(alpha = 0.08f),
        inactiveContentColor = TEXT_DIM,
        inactiveBorderColor = Color.Transparent,
    )
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = selected == ApiClient.CATEGORY_MOVIES,
            onClick = { onSelect(ApiClient.CATEGORY_MOVIES) },
            shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
            colors = chipColors,
            icon = {},
            label = { Text("Movies", fontWeight = FontWeight.Medium) },
        )
        SegmentedButton(
            selected = selected == ApiClient.CATEGORY_TV,
            onClick = { onSelect(ApiClient.CATEGORY_TV) },
            shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
            colors = chipColors,
            icon = {},
            label = { Text("TV", fontWeight = FontWeight.Medium) },
        )
    }
}

private val BG = Color(0xFF111111)
private val GREEN = Color(0xFF22C55E)
private val ORANGE = Color(0xFFC2410C)
private val TEXT_DIM = Color.White.copy(alpha = 0.5f)
private val SEPARATOR = Color.White.copy(alpha = 0.07f)

private val QUALITY_BADGE: Map<String, Pair<Color, Color>> = mapOf(
    "4K"    to (Color(0xFFEA580C) to Color.White),
    "1080p" to (Color(0xFF16A34A) to Color.White),
    "720p"  to (Color(0xFF15803D) to Color.White),
    "480p"  to (Color(0xFF71717A) to Color.White),
    "SD"    to (Color(0xFF3F3F46) to Color(0xFFA1A1AA)),
)

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
    val hasSearched by vm.hasSearched.collectAsStateWithLifecycle()

    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            query = initialQuery
            vm.search(initialQuery, selectedCategory)
        } else {
            searchFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(query, selectedCategory) {
        if (query.length >= 2) {
            delay(600)
            vm.search(query, selectedCategory)
        } else if (query.isEmpty()) {
            vm.clearResults()
        }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .padding(horizontal = 48.dp, vertical = 28.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search movies & TV shows…", color = TEXT_DIM) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search(query, selectedCategory) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.White.copy(alpha = 0.06f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester),
            )
            CategoryToggle(
                selected = selectedCategory,
                onSelect = { selectedCategory = it },
            )
        }

        Spacer(Modifier.height(24.dp))

        when {
            isSearching || isStartingStream -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GREEN)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (isStartingStream) "Starting stream…" else "Searching…",
                            color = TEXT_DIM,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = Color(0xFFf87171), fontSize = 14.sp)
                }
            }
            hasSearched && results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No results found.", color = TEXT_DIM, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Try a different title or check spelling.", color = TEXT_DIM.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            }
            results.isNotEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SEPARATOR),
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results) { result ->
                        ResultRow(result = result, onClick = { vm.startStream(result) })
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(SEPARATOR),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
    val (badgeBg, badgeFg) = QUALITY_BADGE[result.quality] ?: QUALITY_BADGE["SD"]!!

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = CardDefaults.shape(RoundedCornerShape(6.dp)),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            focusedContainerColor = GREEN.copy(alpha = 0.1f),
            focusedContentColor = Color.White,
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, GREEN.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(6.dp),
            ),
        ),
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${formatSize(result.size)}  ·  ${result.seeders} seeders  ·  ${result.indexer}",
                    fontSize = 12.sp,
                    color = TEXT_DIM,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = result.quality,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg,
                )
            }
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val gb = bytes / 1_073_741_824.0
    val mb = bytes / 1_048_576.0
    return if (gb >= 1) "%.1f GB".format(gb) else "%.0f MB".format(mb)
}

private fun infoHashFromMagnet(magnetUrl: String): String =
    magnetUrl
        .substringAfter("urn:btih:", "")
        .substringBefore("&")
        .lowercase()
