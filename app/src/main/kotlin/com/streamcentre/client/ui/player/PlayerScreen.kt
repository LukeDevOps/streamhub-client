package com.streamcentre.client.ui.player

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.streamcentre.client.api.ApiClient
import com.streamcentre.client.app
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    hlsUrl: String,
    contentId: String,
    infoHash: String,
    durationSeconds: Float,
    onBack: () -> Unit,
    api: ApiClient = LocalContext.current.app.api,
    vm: PlayerViewModel = viewModel(factory = PlayerViewModel.factory(api)),
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.Builder()
                .setUri(hlsUrl)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
            setMediaItem(mediaItem)
            prepare()
        }
    }

    var controlsVisible by remember { mutableStateOf(true) }
    // Incrementing this key resets the auto-hide timer
    var controlsTimerKey by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf((durationSeconds * 1000).toLong()) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val d = exoPlayer.duration
                    if (d > 0) durationMs = d
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Load resume position and start playback
    LaunchedEffect(Unit) {
        vm.initialize(contentId, infoHash)
        val resumePos = vm.getResumePosition()
        if (resumePos > 0) exoPlayer.seekTo(resumePos * 1000L)
        exoPlayer.playWhenReady = true
        vm.scrobble("start", 0.0)
    }

    // Position ticker + periodic save
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            delay(1000)
            currentPositionMs = exoPlayer.currentPosition
            if (exoPlayer.isPlaying) {
                vm.savePosition(exoPlayer.currentPosition, exoPlayer.duration.coerceAtLeast(durationMs))
            }
        }
    }

    // Auto-hide controls after 3 seconds of inactivity
    LaunchedEffect(controlsTimerKey) {
        delay(3000)
        controlsVisible = false
    }

    DisposableEffect(Unit) {
        onDispose {
            val dur = exoPlayer.duration.takeIf { it > 0 } ?: durationMs
            val progress = if (dur > 0) {
                (exoPlayer.currentPosition.toDouble() / dur * 100).coerceIn(0.0, 100.0)
            } else 0.0
            vm.scrobble("stop", progress)
            vm.savePosition(exoPlayer.currentPosition, dur)
            exoPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.Back -> { onBack(); true }

                    Key.DirectionCenter, Key.Enter -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        controlsVisible = true
                        controlsTimerKey++
                        true
                    }

                    Key.DirectionLeft -> {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
                        controlsVisible = true
                        controlsTimerKey++
                        true
                    }

                    Key.DirectionRight -> {
                        val max = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                        exoPlayer.seekTo((exoPlayer.currentPosition + 10_000).coerceAtMost(max))
                        controlsVisible = true
                        controlsTimerKey++
                        true
                    }

                    Key.DirectionUp -> {
                        controlsVisible = true
                        controlsTimerKey++
                        true
                    }

                    Key.DirectionDown -> {
                        controlsVisible = false
                        true
                    }

                    Key.MediaPlayPause -> {
                        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                        true
                    }

                    Key.MediaFastForward -> {
                        val max = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                        exoPlayer.seekTo((exoPlayer.currentPosition + 30_000).coerceAtMost(max))
                        controlsVisible = true
                        controlsTimerKey++
                        true
                    }

                    Key.MediaRewind -> {
                        exoPlayer.seekTo((exoPlayer.currentPosition - 30_000).coerceAtLeast(0))
                        controlsVisible = true
                        controlsTimerKey++
                        true
                    }

                    else -> false
                }
            },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            ControlsOverlay(
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
            )
        }
    }
}

@Composable
private fun ControlsOverlay(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    startY = Float.MAX_VALUE * 0.4f,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 32.dp),
        ) {
            LinearProgressIndicator(
                progress = {
                    if (durationMs > 0) currentPositionMs.toFloat() / durationMs else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatMs(currentPositionMs),
                    color = Color.White,
                    fontSize = 16.sp,
                )
                Text(
                    text = if (isPlaying) "▶" else "⏸",
                    color = Color.White,
                    fontSize = 20.sp,
                )
                Text(
                    text = formatMs(durationMs),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}
