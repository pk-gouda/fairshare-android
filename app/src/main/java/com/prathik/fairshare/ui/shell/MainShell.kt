package com.prathik.fairshare.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prathik.fairshare.ui.navigation.PlaceholderScreen
import com.prathik.fairshare.ui.navigation.Screen
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface1
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

/**
 * Data class representing a bottom navigation tab.
 *
 * [route]         — the root destination of this tab
 * [selectedIcon]  — filled icon shown when tab is active
 * [unselectedIcon]— outlined icon shown when tab is inactive
 * [label]         — text label below the icon
 */
data class BottomNavItem(
    val route        : String,
    val selectedIcon : ImageVector,
    val unselectedIcon: ImageVector,
    val label        : String,
)

/**
 * Main shell — wraps the app's core navigation structure.
 *
 * Contains:
 * - Scaffold with a bottom NavigationBar
 * - 4 tabs: Groups, Friends, Activity (with unread badge), Account
 * - Each tab has its own independent NavController + back stack
 *
 * Back stack behavior:
 * - Switching tabs saves and restores each tab's back stack
 * - Pressing back within a tab navigates back within that tab
 * - Tapping the current tab scrolls to top / pops to root (launchSingleTop)
 */
@Composable
fun MainShell(
    rootNavController: NavController,
    viewModel        : MainShellViewModel = hiltViewModel(),
) {
    val unreadCount by viewModel.unreadCount.collectAsState()

    val tabs = listOf(
        BottomNavItem(
            route          = Screen.Groups.route,
            selectedIcon   = Icons.Filled.Groups,
            unselectedIcon = Icons.Outlined.Groups,
            label          = "Groups",
        ),
        BottomNavItem(
            route          = Screen.Friends.route,
            selectedIcon   = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People,
            label          = "Friends",
        ),
        BottomNavItem(
            route          = Screen.Activity.route,
            selectedIcon   = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications,
            label          = "Activity",
        ),
        BottomNavItem(
            route          = Screen.Account.route,
            selectedIcon   = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle,
            label          = "Account",
        ),
    )

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val shellNavController = rememberNavController()

    Scaffold(
        containerColor = Surface0,
        bottomBar = {
            NavigationBar(
                containerColor = Surface1,
                tonalElevation = androidx.compose.ui.unit.Dp.Unspecified,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected  = isSelected,
                        onClick   = {
                            if (selectedTabIndex == index) {
                                // Already on this tab — pop to root
                                shellNavController.popBackStack(
                                    route     = tab.route,
                                    inclusive = false,
                                )
                            } else {
                                selectedTabIndex = index
                                shellNavController.navigate(tab.route) {
                                    popUpTo(shellNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            }
                        },
                        icon = {
                            if (tab.route == Screen.Activity.route && unreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(
                                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                fontSize   = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector        = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector        = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            }
                        },
                        label = {
                            Text(
                                text       = tab.label,
                                fontSize   = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Green400,
                            selectedTextColor   = Green400,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor      = Surface4,
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = shellNavController,
            startDestination = Screen.Groups.route,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Groups.route) {
                PlaceholderScreen("Groups")
            }
            composable(Screen.Friends.route) {
                PlaceholderScreen("Friends")
            }
            composable(Screen.Activity.route) {
                PlaceholderScreen("Activity")
            }
            composable(Screen.Account.route) {
                PlaceholderScreen("Account")
            }
        }
    }
}