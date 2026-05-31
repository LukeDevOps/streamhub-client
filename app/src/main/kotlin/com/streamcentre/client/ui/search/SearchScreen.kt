package com.streamcentre.client.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.Context
import android.media.AudioManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.streamcentre.client.api.Suggestion
import com.streamcentre.client.app
import kotlinx.coroutines.delay

private val BG       = Color(0xFF111111)
private val TEXT_DIM = Color.White.copy(alpha = 0.5f)
private val SEPARATOR = Color.White.copy(alpha = 0.07f)

private val QUALITY_BADGE: Map<String, Pair<Color, Color>> = mapOf(
    "4K"    to (Color(0xFFEA580C) to Color.White),
    "1080p" to (Color(0xFF16A34A) to Color.White),
    "720p"  to (Color(0xFF15803D) to Color.White),
    "480p"  to (Color(0xFF71717A) to Color.White),
    "SD"    to (Color(0xFF3F3F46) to Color(0xFFA1A1AA)),
)

private enum class KbMode { ALPHA, SYMBOLS }

private val ALPHA_ROWS = listOf(
    listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
    listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
    listOf("Z", "X", "C", "V", "B", "N", "M"),
)

private val SYMBOL_ROWS = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
    listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"),
    listOf("*", "\"", "'", ":", ";", "!", "?", ",", "."),
)

private val LED_GREEN = Color(0xFF4ADE80)

@Composable
private fun KeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    isActive: Boolean = false,
    hasLed: Boolean = false,
    ledOn: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val textColor = if (focused) Color.Black else Color.White
    val audioManager = LocalContext.current.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    Card(
        onClick = {
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
            onClick()
        },
        modifier = modifier
            .height(42.dp)
            .onFocusChanged { focused = it.isFocused },
        shape = CardDefaults.shape(RoundedCornerShape(6.dp)),
        colors = CardDefaults.colors(
            containerColor = if (isAction || isActive) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.07f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = CardDefaults.scale(focusedScale = 1.06f),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (hasLed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(if (ledOn) LED_GREEN else Color.Transparent, CircleShape),
                    )
                    Text(text = label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            } else {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = if (label.length > 2) 10.sp else 14.sp,
                    fontWeight = if (isAction) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: Suggestion, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(34.dp).onFocusChanged { focused = it.isFocused },
        shape = CardDefaults.shape(RoundedCornerShape(6.dp)),
        colors = CardDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = CardDefaults.border(border = Border.None, focusedBorder = Border.None),
        scale = CardDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val textColor = if (focused) Color.Black else Color.White
            Text(
                text = suggestion.title,
                color = textColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (suggestion.year.isNotEmpty()) {
                Text(
                    text = suggestion.year,
                    color = if (focused) Color.Black.copy(alpha = 0.5f) else TEXT_DIM,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun TvKeyboard(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<Suggestion>,
    onSuggestionClick: (Suggestion) -> Unit,
    selectedCategory: Int,
    onCategorySelect: (Int) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(KbMode.ALPHA) }
    var capsLock by remember { mutableStateOf(false) }
    val rows = if (mode == KbMode.ALPHA) ALPHA_ROWS else SYMBOL_ROWS

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // Query display — always at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
        ) {
            Text(
                text = if (query.isEmpty()) "Search…" else query,
                color = if (query.isEmpty()) TEXT_DIM else Color.White,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Suggestions fill the gap — clipped so they never push the keyboard
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            contentAlignment = Alignment.TopStart,
        ) {
            if (suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    suggestions.forEach { suggestion ->
                        SuggestionRow(suggestion, onClick = { onSuggestionClick(suggestion) })
                    }
                }
            }
        }

        // Row 0 – 10 keys (Q-P or 1-0), no stagger
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            rows[0].forEach { key ->
                val display = if (mode == KbMode.ALPHA && !capsLock) key.lowercase() else key
                KeyButton(display, modifier = Modifier.weight(1f), onClick = { onQueryChange(query + display) })
            }
        }

        // Row 1 – 9 keys (A-L or @-/), half-key stagger each side
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Spacer(Modifier.weight(0.5f))
            rows[1].forEach { key ->
                val display = if (mode == KbMode.ALPHA && !capsLock) key.lowercase() else key
                KeyButton(display, modifier = Modifier.weight(1f), onClick = { onQueryChange(query + display) })
            }
            Spacer(Modifier.weight(0.5f))
        }

        // Row 2 – ⇪ + 7 keys (Z-M or *-.) + ⌫, matching system keyboard layout
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (mode == KbMode.ALPHA) {
                KeyButton("⇪", modifier = Modifier.weight(1.5f), isAction = true, isActive = capsLock,
                    hasLed = true, ledOn = capsLock, onClick = { capsLock = !capsLock })
            } else {
                Spacer(Modifier.weight(1.5f))
            }
            rows[2].forEach { key ->
                val display = if (mode == KbMode.ALPHA && !capsLock) key.lowercase() else key
                KeyButton(display, modifier = Modifier.weight(1f), onClick = { onQueryChange(query + display) })
            }
            KeyButton("⌫", modifier = Modifier.weight(1.5f), isAction = true, onClick = { onQueryChange(query.dropLast(1)) })
        }

        // Bottom action row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val modeLabel = if (mode == KbMode.ALPHA) "?123" else "ABC"
            KeyButton(modeLabel, modifier = Modifier.weight(2f), isAction = true, onClick = {
                mode = if (mode == KbMode.ALPHA) KbMode.SYMBOLS else KbMode.ALPHA
            })
            KeyButton("SPACE", modifier = Modifier.weight(4f), onClick = { onQueryChange("$query ") })
            KeyButton("⏎", modifier = Modifier.weight(2f), isAction = true, onClick = onSearch)
        }

        Spacer(Modifier.height(6.dp))

        // Category toggle
        val chipColors = SegmentedButtonDefaults.colors(
            activeContainerColor = Color.White,
            activeContentColor = Color.Black,
            activeBorderColor = Color.Transparent,
            inactiveContainerColor = Color.White.copy(alpha = 0.08f),
            inactiveContentColor = TEXT_DIM,
            inactiveBorderColor = Color.Transparent,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedCategory == ApiClient.CATEGORY_MOVIES,
                onClick = { onCategorySelect(ApiClient.CATEGORY_MOVIES) },
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp),
                colors = chipColors,
                icon = {},
                label = { Text("Movies", fontWeight = FontWeight.Medium) },
            )
            SegmentedButton(
                selected = selectedCategory == ApiClient.CATEGORY_TV,
                onClick = { onCategorySelect(ApiClient.CATEGORY_TV) },
                shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                colors = chipColors,
                icon = {},
                label = { Text("TV", fontWeight = FontWeight.Medium) },
            )
        }
    }
}

@Composable
fun SearchScreen(
    initialQuery: String = "",
    initialCategory: Int = ApiClient.CATEGORY_MOVIES,
    onPlay: (url: String, contentId: String, infoHash: String, duration: Float) -> Unit,
    api: ApiClient = LocalContext.current.app.api,
    vm: SearchViewModel = viewModel(factory = SearchViewModel.factory(api)),
) {
    var query by remember { mutableStateOf(initialQuery) }
    var selectedCategory by remember { mutableIntStateOf(initialCategory) }
    var suggestions by remember { mutableStateOf<List<Suggestion>>(emptyList()) }
    var minSuggestionLen by remember { mutableIntStateOf(0) }
    var lockedQuery by remember { mutableStateOf("") }

    val results by vm.results.collectAsStateWithLifecycle()
    val isSearching by vm.isSearching.collectAsStateWithLifecycle()
    val isStartingStream by vm.isStartingStream.collectAsStateWithLifecycle()
    val streamResult by vm.streamResult.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val hasSearched by vm.hasSearched.collectAsStateWithLifecycle()

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            query = initialQuery
            vm.search(initialQuery, selectedCategory)
        }
    }

    // Reset suggestion gate whenever the category flips so we always refetch fresh.
    LaunchedEffect(selectedCategory) {
        suggestions = emptyList()
        minSuggestionLen = 0
    }

    LaunchedEffect(query, selectedCategory) {
        if (query.length >= 4) {
            val shouldFetch = query != lockedQuery && (minSuggestionLen == 0 || query.length >= minSuggestionLen)
            delay(300)
            if (shouldFetch) {
                try {
                    val result = api.suggest(query, selectedCategory)
                    suggestions = result
                    minSuggestionLen = if (result.isNotEmpty()) result.minOf { it.title.length } else 0
                } catch (_: Exception) { }
            }
            delay(300)
            vm.search(query, selectedCategory)
        } else {
            suggestions = emptyList()
            minSuggestionLen = 0
            if (query.isEmpty()) vm.clearResults()
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .padding(horizontal = 48.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        // Left 1/3: permanent keyboard
        TvKeyboard(
            query = query,
            onQueryChange = { query = it },
            suggestions = suggestions,
            onSuggestionClick = { s ->
                lockedQuery = s.title
                query = s.title
                suggestions = emptyList()
                vm.search(s.title, selectedCategory)
            },
            selectedCategory = selectedCategory,
            onCategorySelect = { selectedCategory = it },
            onSearch = { vm.search(query, selectedCategory) },
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )

        // Right 2/3: results
        Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
            when {
                isSearching || isStartingStream -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
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
                    Column {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SEPARATOR))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(results) { result ->
                                ResultRow(result = result, onClick = { vm.startStream(result) })
                                Box(Modifier.fillMaxWidth().height(1.dp).background(SEPARATOR))
                            }
                        }
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
            focusedContainerColor = Color.White.copy(alpha = 0.08f),
            focusedContentColor = Color.White,
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(6.dp),
            ),
        ),
        scale = CardDefaults.scale(focusedScale = 1.03f),
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
