package com.streamcentre.client.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.streamcentre.client.api.ApiClient
import com.streamcentre.client.ui.browse.BrowseScreen
import com.streamcentre.client.ui.player.PlayerScreen
import com.streamcentre.client.ui.search.SearchScreen

@Composable
fun AppNav(api: ApiClient) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "browse") {

        composable("browse") {
            BrowseScreen(
                api = api,
                onSearchClick = { nav.navigate("search") },
                onItemSelected = { title, category -> nav.navigate("search?query=${Uri.encode(title)}&category=$category") },
            )
        }

        composable(
            route = "search?query={query}&category={category}",
            arguments = listOf(
                navArgument("query") { defaultValue = "" },
                navArgument("category") { type = NavType.IntType; defaultValue = ApiClient.CATEGORY_MOVIES },
            ),
        ) { back ->
            val query = back.arguments?.getString("query") ?: ""
            val category = back.arguments?.getInt("category") ?: ApiClient.CATEGORY_MOVIES
            SearchScreen(
                api = api,
                initialQuery = query,
                initialCategory = category,
                onPlay = { url, contentId, infoHash, duration ->
                    nav.navigate(
                        "player?" +
                            "url=${Uri.encode(url)}" +
                            "&contentId=${Uri.encode(contentId)}" +
                            "&infoHash=${Uri.encode(infoHash)}" +
                            "&duration=$duration"
                    )
                },
            )
        }

        composable(
            route = "player?url={url}&contentId={contentId}&infoHash={infoHash}&duration={duration}",
            arguments = listOf(
                navArgument("url") { defaultValue = "" },
                navArgument("contentId") { defaultValue = "" },
                navArgument("infoHash") { defaultValue = "" },
                navArgument("duration") { type = NavType.FloatType; defaultValue = 0f },
            ),
        ) { back ->
            PlayerScreen(
                api = api,
                hlsUrl = back.arguments?.getString("url") ?: "",
                contentId = back.arguments?.getString("contentId") ?: "",
                infoHash = back.arguments?.getString("infoHash") ?: "",
                durationSeconds = back.arguments?.getFloat("duration") ?: 0f,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
