package com.cocode.linkqrwallet.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import com.cocode.linkqrwallet.LinkQrWalletApp
import com.cocode.linkqrwallet.ui.screens.AddLinkScreen
import com.cocode.linkqrwallet.ui.screens.AboutScreen
import com.cocode.linkqrwallet.ui.screens.DetailScreen
import com.cocode.linkqrwallet.ui.screens.LibraryScreen
import com.cocode.linkqrwallet.ui.screens.ScanQrScreen
import com.cocode.linkqrwallet.ui.viewmodel.AppViewModelFactory

@Composable
fun LinkQrWalletRoot(sharedUrl: String?) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val repository = (context.applicationContext as LinkQrWalletApp).repository
    val viewModelFactory = remember(repository) { AppViewModelFactory(repository) }
    var handledShare by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank() && !handledShare) {
            handledShare = true
            val encoded = Uri.encode(sharedUrl)
            navController.navigate("add?url=$encoded")
        }
    }

    NavHost(navController = navController, startDestination = "library") {
        composable("library") {
            LibraryScreen(
                viewModelFactory = viewModelFactory,
                onAdd = { navController.navigate("add") },
                onScan = { navController.navigate("scan") },
                onAbout = { navController.navigate("about") },
                onOpenDetail = { id -> navController.navigate("detail/$id") }
            )
        }
        composable(
            route = "add?url={url}",
            arguments = listOf(navArgument("url") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url").orEmpty()
            AddLinkScreen(
                viewModelFactory = viewModelFactory,
                initialUrl = url,
                onDone = { id ->
                    navController.navigate("detail/$id") {
                        popUpTo("library")
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            DetailScreen(
                viewModelFactory = viewModelFactory,
                linkId = id,
                onBack = { navController.popBackStack() }
            )
        }
        composable("scan") {
            ScanQrScreen(
                onResult = { result ->
                    val encoded = Uri.encode(result)
                    navController.navigate("add?url=$encoded") {
                        popUpTo("library")
                    }
                },
                onClose = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
