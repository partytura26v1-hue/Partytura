package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.firebase.RepertoireRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.signOut
import com.example.ui.viewmodel.RepertoireViewModel
import com.example.ui.viewmodel.SongStudioViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object Routes {
    const val LOOKUP = "lookup"
    const val REPERTOIRE = "repertoire"
    const val SETLISTS = "setlists"
    const val STUDIO = "studio/{songId}?tab={tab}"
    fun studio(songId: String, initialTab: Int) = "studio/$songId?tab=$initialTab"
}

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopDestinations = listOf(
    TopDestination(
        route = Routes.LOOKUP,
        labelRes = R.string.nav_lookup,
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search,
        testTag = "nav_lookup"
    ),
    TopDestination(
        route = Routes.REPERTOIRE,
        labelRes = R.string.nav_repertoire,
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic,
        testTag = "nav_repertoire"
    ),
    TopDestination(
        route = Routes.SETLISTS,
        labelRes = R.string.nav_setlists,
        selectedIcon = Icons.AutoMirrored.Filled.QueueMusic,
        unselectedIcon = Icons.AutoMirrored.Outlined.QueueMusic,
        testTag = "nav_setlists"
    )
)

@Composable
fun PartyturaApp() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedBandShell(
                currentUserId = user.uid,
                userEmail = user.email
            )
        }
    }
}

@Composable
private fun AuthenticatedBandShell(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val repertoireViewModel: RepertoireViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                RepertoireViewModel(RepertoireRepository(db), currentUserId)
            }
        }
    )

    val lookupState by repertoireViewModel.lookupState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lookupState.bannerMessage) {
        val msg = lookupState.bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            repertoireViewModel.clearBannerMessage()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute in TopDestinations.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(Routes.LOOKUP) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        TopDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(Routes.LOOKUP) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Routes.LOOKUP,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(Routes.LOOKUP) {
                        LookupScreen(
                            viewModel = repertoireViewModel,
                            userEmail = userEmail,
                            onSignOut = {
                                signOut(credentialManager, scope)
                            },
                            onOpenSong = { songId, tab ->
                                navController.navigate(Routes.studio(songId, tab))
                            }
                        )
                    }

                    composable(Routes.REPERTOIRE) {
                        RepertoireScreen(
                            viewModel = repertoireViewModel,
                            onOpenSong = { songId, tab ->
                                navController.navigate(Routes.studio(songId, tab))
                            },
                            onNavigateToLookup = {
                                navController.navigate(Routes.LOOKUP) {
                                    popUpTo(Routes.LOOKUP) { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Routes.SETLISTS) {
                        SetlistsScreen(
                            viewModel = repertoireViewModel,
                            onOpenSong = { songId, tab ->
                                navController.navigate(Routes.studio(songId, tab))
                            }
                        )
                    }

                    composable(
                        route = Routes.STUDIO,
                        arguments = listOf(
                            navArgument("songId") { type = NavType.StringType },
                            navArgument("tab") {
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        )
                    ) { entry ->
                        val songId = entry.arguments?.getString("songId").orEmpty()
                        val initialTab = entry.arguments?.getInt("tab") ?: 0
                        val studioVm: SongStudioViewModel = viewModel(
                            key = "${currentUserId}_$songId",
                            factory = viewModelFactory {
                                initializer {
                                    val app = checkNotNull(this[APPLICATION_KEY])
                                    val dbId = app.getString(R.string.firestore_database_id)
                                    val db = FirebaseFirestore.getInstance(dbId)
                                    SongStudioViewModel(RepertoireRepository(db), currentUserId, songId)
                                }
                            }
                        )
                        SongStudioScreen(
                            viewModel = studioVm,
                            initialTab = initialTab,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
