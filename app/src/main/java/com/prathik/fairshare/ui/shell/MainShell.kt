package com.prathik.fairshare.ui.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.prathik.fairshare.ui.expense.AddExpenseScreen
import com.prathik.fairshare.ui.groups.GroupDetailScreen
import com.prathik.fairshare.ui.groups.GroupSettingsScreen
import com.prathik.fairshare.ui.groups.GroupsHomeScreen
import com.prathik.fairshare.ui.friends.FriendsScreen
import com.prathik.fairshare.ui.friends.AddFriendByEmailScreen
import com.prathik.fairshare.ui.friends.QrCodeScreen
import com.prathik.fairshare.ui.friends.ScanQrCodeScreen
import com.prathik.fairshare.ui.activity.ActivityScreen
import com.prathik.fairshare.ui.account.AccountScreen
import com.prathik.fairshare.ui.account.AccountViewModel
import com.prathik.fairshare.ui.search.GlobalSearchScreen
import com.prathik.fairshare.ui.navigation.PlaceholderScreen
import com.prathik.fairshare.ui.navigation.Screen
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface1
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.expense.CurrencySelectScreen
import com.prathik.fairshare.ui.expense.AddExpenseViewModel
import com.prathik.fairshare.ui.expense.ExpenseDetailScreen
import com.prathik.fairshare.ui.settlement.SettleUpScreen


/**
 * Data class representing a bottom navigation tab.
 */
data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
)

/**
 * Main shell — wraps the app's core navigation structure.
 *
 * Contains:
 * - Scaffold with a bottom NavigationBar
 * - 4 tabs: Groups, Friends, Activity (with unread badge), Account
 * - Shell NavHost handles ALL app screens — placeholders replaced
 *   as we build each screen day by day.
 *
 * NavGraph.kt only handles auth flow + hands off here.
 * All 39 app routes are registered in this NavHost.
 */
@Composable
fun MainShell(
    rootNavController: NavController,
    viewModel: MainShellViewModel = hiltViewModel(),
) {
    val unreadCount by viewModel.unreadCount.collectAsState()

    val tabs = listOf(
        BottomNavItem(
            route = Screen.Groups.route,
            selectedIcon = Icons.Filled.Groups,
            unselectedIcon = Icons.Outlined.Groups,
            label = "Groups",
        ),
        BottomNavItem(
            route = Screen.Friends.route,
            selectedIcon = Icons.Filled.People,
            unselectedIcon = Icons.Outlined.People,
            label = "Friends",
        ),
        BottomNavItem(
            route = Screen.Activity.route,
            selectedIcon = Icons.Filled.Notifications,
            unselectedIcon = Icons.Outlined.Notifications,
            label = "Activity",
        ),
        BottomNavItem(
            route = Screen.Account.route,
            selectedIcon = Icons.Filled.AccountCircle,
            unselectedIcon = Icons.Outlined.AccountCircle,
            label = "Account",
        ),
    )

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val shellNavController = rememberNavController()

    // Keep selectedTabIndex in sync with the actual back stack
    val currentBackStackEntry by shellNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        val index = tabs.indexOfFirst { currentRoute == it.route }
        if (index >= 0) selectedTabIndex = index
    }

    Scaffold(
        containerColor = Surface0,
        bottomBar = {
            Box {
                NavigationBar(
                    containerColor = Surface1,
                    tonalElevation = androidx.compose.ui.unit.Dp.Unspecified,
                ) {
                    // First 2 tabs: Groups, Friends
                    tabs.take(2).forEachIndexed { index, tab ->
                        val isSelected = selectedTabIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (selectedTabIndex == index) {
                                    shellNavController.popBackStack(tab.route, false)
                                } else {
                                    selectedTabIndex = index
                                    shellNavController.navigate(tab.route) {
                                        popUpTo(shellNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green400,
                                selectedTextColor = Green400,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Surface4,
                            ),
                        )
                    }

                    // Center placeholder for the elevated search button
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Box(modifier = Modifier.size(46.dp)) },
                        label = {},
                        enabled = false,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )

                    // Last 2 tabs: Activity, Account
                    tabs.takeLast(2).forEachIndexed { i, tab ->
                        val index = i + 2
                        val isSelected = selectedTabIndex == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (selectedTabIndex == index) {
                                    shellNavController.popBackStack(tab.route, false)
                                } else {
                                    selectedTabIndex = index
                                    shellNavController.navigate(tab.route) {
                                        popUpTo(shellNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                if (tab.route == Screen.Activity.route && unreadCount > 0) {
                                    BadgedBox(badge = {
                                        Badge {
                                            Text(
                                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label,
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Green400,
                                selectedTextColor = Green400,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Surface4,
                            ),
                        )
                    }
                }

                // Elevated center search button — sits above the nav bar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-16).dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Green400)
                        .clickable {
                            shellNavController.navigate(Screen.Search.route())
                        },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search",
                        tint = Surface0,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = shellNavController,
            startDestination = Screen.Groups.route,
            modifier = Modifier.padding(innerPadding),
        ) {

            // ── Tab roots ─────────────────────────────────────────────────────
            composable(Screen.Groups.route) {
                GroupsHomeScreen(
                    onNavigateToGroup = { groupId ->
                        shellNavController.navigate(Screen.GroupDetail.route(groupId))
                    },
                    onNavigateToSearch = {
                        shellNavController.navigate(Screen.Search.route())
                    },
                    onNavigateToCreateGroup = {
                        shellNavController.navigate(Screen.CreateGroup.route)
                    },
                    onNavigateToAddExpense = {                              // ← ADD THIS
                        shellNavController.navigate(Screen.AddExpense.route())
                    },
                )
            }
            composable(Screen.Friends.route) {
                FriendsScreen(
                    onNavigateToAddFriendByEmail = { shellNavController.navigate(Screen.AddFriendByEmail.route) },
                    onNavigateToScanQr = { shellNavController.navigate(Screen.ScanQrCode.route) },
                    onNavigateToImport = { shellNavController.navigate(Screen.ImportSplitwise.route) },
                    onNavigateToRequests = { shellNavController.navigate(Screen.FriendRequests.route) },
                    onNavigateToFriend = { friendId ->
                        shellNavController.navigate(
                            Screen.FriendDetail.route(
                                friendId
                            )
                        )
                    },
                )
            }
            composable(Screen.Activity.route) {
                ActivityScreen(
                    onNavigateToExpense = { expenseId ->
                        shellNavController.navigate(Screen.ExpenseDetail.route(expenseId))
                    },
                    onNavigateToFriend = {
                        shellNavController.navigate(Screen.Friends.route)
                    },
                    onNavigateToGroup = { groupId ->
                        shellNavController.navigate(Screen.GroupDetail.route(groupId))
                    },
                )
            }
            composable(Screen.Account.route) {
                AccountScreen(
                    onNavigateToEditProfile = { shellNavController.navigate(Screen.EditProfile.route) },
                    onNavigateToQrCode = { shellNavController.navigate(Screen.QrCode.route) },
                    onNavigateToCurrency = { shellNavController.navigate(Screen.CurrencySelect.route) },
                    onNavigateToChangePassword = { shellNavController.navigate(Screen.ChangePassword.route) },
                    onNavigateToMyAnalytics = { shellNavController.navigate(Screen.MyAnalytics.route) },
                    onNavigateToImport = { shellNavController.navigate(Screen.ImportSplitwise.route) },
                    onLoggedOut = {
                        rootNavController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Groups.route) { inclusive = true }
                        }
                    },
                )
            }
            // ── Group screens ─────────────────────────────────────────────────
            composable(
                route = Screen.GroupDetail.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                GroupDetailScreen(
                    onBack = { shellNavController.popBackStack() },
                    onNavigateToSettings = { groupId ->
                        shellNavController.navigate(Screen.GroupSettings.route(groupId))
                    },
                    onNavigateToSearch = { groupId ->
                        shellNavController.navigate(Screen.Search.route(groupId))
                    },
                    onNavigateToExpense = { expenseId ->
                        shellNavController.navigate(Screen.ExpenseDetail.route(expenseId))
                    },
                    onNavigateToAddExpense = { groupId ->
                        shellNavController.navigate(Screen.AddExpense.route(groupId))
                    },
                    onNavigateToSettle = { otherUserId ->
                        shellNavController.navigate(Screen.SettleUp.route(otherUserId))
                    },
                )
            }
            composable(
                route = Screen.GroupSettings.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                GroupSettingsScreen(
                    onBack = { shellNavController.popBackStack() },
                    onNavigateToAddMember = { groupId ->
                        shellNavController.navigate(Screen.AddMember.route(groupId))
                    },
                    onGroupDeleted = {
                        shellNavController.popBackStack(Screen.Groups.route, false)
                    },
                )
            }

            composable(
                route = Screen.GroupMembers.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Group Members") }

            composable(
                route = Screen.AddMember.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Add Member") }

            composable(
                route = Screen.WhoOwesWho.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Who Owes Who") }

            composable(
                route = Screen.TotalsSheet.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Totals Sheet") }

            composable(
                route = Screen.GroupAnalytics.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Group Analytics") }

            composable(Screen.CreateGroup.route) {
                PlaceholderScreen("Create Group")
            }

            composable(
                route = Screen.JoinGroup.route,
                arguments = listOf(
                    navArgument("inviteCode") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { PlaceholderScreen("Join Group") }

            composable(
                route = Screen.RecurringExpenses.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Recurring Expenses") }

            composable(
                route = Screen.Reminders.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Reminders") }

            composable(
                route = Screen.CreateReminder.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Create Reminder") }

            // ── Expense screens ───────────────────────────────────────────────
            composable(
                route = Screen.AddExpense.route,
                arguments = listOf(
                    navArgument("groupId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                AddExpenseScreen(
                    onBack = { shellNavController.popBackStack() },
                    onSuccess = { shellNavController.popBackStack() },
                    onNavigateToCurrency = {
                        shellNavController.navigate(Screen.CurrencySelect.route)
                    },
                )
            }
            composable(Screen.CurrencySelect.route) { backStackEntry ->
                // Check if we came from AddExpense (has that route in back stack)
                val fromAddExpense = remember(backStackEntry) {
                    try {
                        shellNavController.getBackStackEntry(Screen.AddExpense.route)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (fromAddExpense) {
                    val parentEntry = remember(backStackEntry) {
                        shellNavController.getBackStackEntry(Screen.AddExpense.route)
                    }
                    val addExpenseViewModel = hiltViewModel<AddExpenseViewModel>(parentEntry)
                    val currency by addExpenseViewModel.currency.collectAsState()
                    CurrencySelectScreen(
                        currentCurrency = currency,
                        onSelect = { selected -> addExpenseViewModel.onCurrencyChanged(selected) },
                        onBack = { shellNavController.popBackStack() },
                    )
                } else {
                    // Coming from Account screen — use AccountViewModel
                    val accountViewModel = hiltViewModel<AccountViewModel>()
                    val profile by accountViewModel.profile.collectAsState()
                    CurrencySelectScreen(
                        currentCurrency = profile?.preferredCurrency ?: "USD",
                        onSelect = { selected ->
                            accountViewModel.updateCurrency(selected)
                        },
                        onBack = { shellNavController.popBackStack() },
                    )
                }
            }

            composable(
                route = Screen.EditExpense.route,
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
            ) { PlaceholderScreen("Edit Expense") }

            composable(
                route = Screen.ExpenseDetail.route,
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
            ) {
                ExpenseDetailScreen(
                    onBack = { shellNavController.popBackStack() },
                    onNavigateToEdit = { expenseId ->
                        shellNavController.navigate(Screen.EditExpense.route(expenseId))
                    },
                    onNavigateToSettle = { otherUserId ->
                        shellNavController.navigate(Screen.SettleUp.route(otherUserId))
                    },
                    onDeleted = { shellNavController.popBackStack() },
                )
            }

            composable(
                route = Screen.ReceiptScan.route,
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
            ) { PlaceholderScreen("Receipt Scan") }

            composable(
                route = Screen.ItemAssignment.route,
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
            ) { PlaceholderScreen("Item Assignment") }

            // ── Settlement screens ────────────────────────────────────────────
            composable(
                route = Screen.SettleUp.route,
                arguments = listOf(
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("groupId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                SettleUpScreen(
                    onBack = { shellNavController.popBackStack() },
                    onSuccess = { shellNavController.popBackStack() },
                )
            }

            composable(
                route = Screen.PartialSettle.route,
                arguments = listOf(
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("groupId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { PlaceholderScreen("Partial Settle") }

            composable(
                route = Screen.SettlementHistory.route,
                arguments = listOf(navArgument("otherUserId") { type = NavType.StringType })
            ) { PlaceholderScreen("Settlement History") }

            // ── Friend screens ────────────────────────────────────────────────
            composable(
                route = Screen.FriendDetail.route,
                arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            ) { PlaceholderScreen("Friend Detail") }

            composable(
                route = Screen.FriendSettings.route,
                arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            ) { PlaceholderScreen("Friend Settings") }

            composable(
                route = Screen.FriendAnalytics.route,
                arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            ) { PlaceholderScreen("Friend Analytics") }

            composable(Screen.AddFriend.route) {
                PlaceholderScreen("Add Friend")
            }

            composable(Screen.AddFriendByEmail.route) {
                AddFriendByEmailScreen(
                    onBack = { shellNavController.popBackStack() },
                )
            }

            composable(Screen.QrCode.route) {
                QrCodeScreen(
                    onBack = { shellNavController.popBackStack() },
                )
            }

            composable(Screen.ScanQrCode.route) {
                ScanQrCodeScreen(
                    onBack = { shellNavController.popBackStack() },
                    onCodeScanned = { code ->
                        // TODO: send friend request by code
                        shellNavController.popBackStack()
                    },
                )
            }

            composable(Screen.FriendRequests.route) {
                PlaceholderScreen("Friend Requests")
            }

            // ── Account screens ───────────────────────────────────────────────
            composable(Screen.EditProfile.route) {
                PlaceholderScreen("Edit Profile")
            }
            composable(Screen.ChangePassword.route) {
                PlaceholderScreen("Change Password")
            }
            composable(Screen.MyAnalytics.route) {
                PlaceholderScreen("My Analytics")
            }
            composable(Screen.ImportSplitwise.route) {
                PlaceholderScreen("Import from Splitwise")
            }

            // ── Search ────────────────────────────────────────────────────────
            composable(
                route = Screen.Search.route,
                arguments = listOf(
                    navArgument("groupId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
            ) {
                GlobalSearchScreen(
                    onBack = { shellNavController.popBackStack() },
                    onNavigateToExpense = { expenseId ->
                        shellNavController.navigate(Screen.ExpenseDetail.route(expenseId))
                    },
                )
            }
        }
    }
}