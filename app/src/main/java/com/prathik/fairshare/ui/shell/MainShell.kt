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
import com.prathik.fairshare.ui.expense.ItemAssignmentScreen
import com.prathik.fairshare.ui.expense.ItemAssignmentViewModel
import com.prathik.fairshare.ui.expense.SaveState
import com.prathik.fairshare.ui.expense.ReviewSubmitScreen
import com.prathik.fairshare.ui.groups.GroupBalancesScreen
import com.prathik.fairshare.ui.groups.GroupDetailScreen
import com.prathik.fairshare.ui.groups.GroupSettingsScreen
import com.prathik.fairshare.ui.groups.GroupSettingsViewModel
import com.prathik.fairshare.ui.groups.GroupsHomeScreen
import com.prathik.fairshare.ui.friends.FriendsScreen
import com.prathik.fairshare.ui.friends.AddFriendScreen
import com.prathik.fairshare.ui.friends.QrCodeScreen
import com.prathik.fairshare.ui.friends.FriendDetailScreen
import com.prathik.fairshare.ui.friends.FriendSettingsScreen
import com.prathik.fairshare.ui.activity.ActivityScreen
import com.prathik.fairshare.ui.account.AccountScreen
import com.prathik.fairshare.ui.account.AccountViewModel
import com.prathik.fairshare.ui.account.ChangePasswordScreen
import com.prathik.fairshare.ui.account.EditProfileScreen
import com.prathik.fairshare.ui.groups.CreateGroupScreen
import com.prathik.fairshare.ui.groups.JoinGroupScreen
import com.prathik.fairshare.ui.groups.AddMemberScreen
import com.prathik.fairshare.ui.search.GlobalSearchScreen
import com.prathik.fairshare.ui.navigation.PlaceholderScreen
import com.prathik.fairshare.ui.groups.GroupMembersScreen
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.ui.groups.RemindersScreen
import com.prathik.fairshare.ui.groups.CreateReminderScreen
import com.prathik.fairshare.ui.groups.RecurringExpensesScreen
import com.prathik.fairshare.ui.groups.GroupAnalyticsScreen
import com.prathik.fairshare.ui.groups.FriendAnalyticsScreen
import com.prathik.fairshare.ui.groups.MyAnalyticsScreen
import com.prathik.fairshare.ui.navigation.Screen
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface1
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.expense.CurrencySelectScreen
import com.prathik.fairshare.ui.expense.AddExpenseViewModel
import com.prathik.fairshare.ui.expense.EditExpenseScreen
import com.prathik.fairshare.ui.expense.EditExpenseViewModel
import com.prathik.fairshare.ui.expense.EditSaveState
import com.prathik.fairshare.ui.expense.ExpenseDetailScreen
import com.prathik.fairshare.ui.expense.ExpenseDetailViewModel
import com.prathik.fairshare.ui.expense.ExpenseDetailUiState
import com.prathik.fairshare.ui.settlement.EditSettlementScreen
import com.prathik.fairshare.ui.settlement.SettlementDetailScreen
import com.prathik.fairshare.ui.settlement.SettleUpScreen
import com.prathik.fairshare.ui.groups.GroupInviteScreen
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.ApiResult
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog


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
    rootNavController : NavController,
    emailChangeToken  : String? = null,
    viewModel         : MainShellViewModel = hiltViewModel(),
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
    val coroutineScope     = rememberCoroutineScope()
    val snackbarHostState  = remember { androidx.compose.material3.SnackbarHostState() }
    // Pending friend add via QR
    var pendingFriendCode by remember { mutableStateOf<String?>(null) }
    var pendingFriend     by remember { mutableStateOf<Friend?>(null) }
    // Pending group join via QR
    var pendingGroupCode  by remember { mutableStateOf<String?>(null) }
    var pendingGroupName  by remember { mutableStateOf<String?>(null) }

    // ── Handle email change deep link when app is already open ────────────────
    // When user taps "Confirm New Email" while app is running, onNewIntent fires
    // and MainShell recomposes with a non-null emailChangeToken.
    LaunchedEffect(emailChangeToken) {
        if (!emailChangeToken.isNullOrBlank()) {
            shellNavController.navigate(Screen.ConfirmEmailChange.route(emailChangeToken)) {
                launchSingleTop = true
            }
        }
    }

    // Keep selectedTabIndex in sync with the actual back stack
    val currentBackStackEntry by shellNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    LaunchedEffect(currentRoute) {
        val index = tabs.indexOfFirst { currentRoute == it.route }
        if (index >= 0) selectedTabIndex = index
    }

    // Routes where bottom nav should be hidden (modal screens)
    // Hide bottom nav on modal screens — check exact static prefixes
    val showBottomBar = when {
        currentRoute == null -> true
        currentRoute.startsWith("add_expense") -> false
        currentRoute.startsWith("expense/") && currentRoute.endsWith("/edit") -> false
        currentRoute.startsWith("settle/") -> false
        currentRoute.startsWith("item_assignment/") -> false
        currentRoute.startsWith("edit_items/") -> false
        currentRoute == "review_submit" -> false
        currentRoute.startsWith("create_group") -> false
        currentRoute == "add_friend" -> false
        currentRoute == "add_friend_email" -> false
        currentRoute.startsWith("group/") && currentRoute.endsWith("/add_member") -> false
        currentRoute.startsWith("group/") && currentRoute.endsWith("/reminders/create") -> false
        currentRoute == "currency_select" -> false
        currentRoute == "scan_qr_code" -> false
        else -> true
    }


    // ── Add friend via QR dialog ──────────────────────────────────────────────
    val capturedFriend = pendingFriend
    val capturedFriendCode = pendingFriendCode
    if (capturedFriend != null && capturedFriendCode != null) {
        AlertDialog(
            onDismissRequest  = { pendingFriendCode = null; pendingFriend = null },
            containerColor    = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
            title = {
                Text("Add ${capturedFriend.fullName}?", fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White)
            },
            text = {
                Text("Do you want to add ${capturedFriend.fullName} as a friend on FairShare?",
                    color = androidx.compose.ui.graphics.Color(0xFF8E8E93), fontSize = 14.sp)
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        pendingFriendCode = null; pendingFriend = null
                        coroutineScope.launch {
                            val result = viewModel.addByFriendCode(capturedFriendCode)
                            when (result) {
                                is ApiResult.Success  -> {
                                    shellNavController.navigate(Screen.Friends.route) {
                                        popUpTo(shellNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    snackbarHostState.showSnackbar("1 friend added!")
                                }
                                is ApiResult.Conflict -> snackbarHostState.showSnackbar("You're already friends with ${capturedFriend.fullName}")
                                is ApiResult.NotFound -> snackbarHostState.showSnackbar("User not found")
                                else                  -> snackbarHostState.showSnackbar("Something went wrong")
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF34C759)),
                ) { Text("Add friend", color = androidx.compose.ui.graphics.Color.Black) }
            },
            dismissButton = {
                TextButton(onClick = { pendingFriendCode = null; pendingFriend = null }) {
                    Text("Cancel", color = androidx.compose.ui.graphics.Color(0xFF8E8E93))
                }
            },
        )
    }

    // ── Join group via QR dialog ──────────────────────────────────────────────
    val capturedGroupCode = pendingGroupCode
    val capturedGroupName = pendingGroupName
    if (capturedGroupCode != null && capturedGroupName != null) {
        AlertDialog(
            onDismissRequest  = { pendingGroupCode = null; pendingGroupName = null },
            containerColor    = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
            title = {
                Text("Join $capturedGroupName?", fontWeight = FontWeight.SemiBold,
                    color = androidx.compose.ui.graphics.Color.White)
            },
            text = {
                Text("Do you want to join $capturedGroupName on FairShare?",
                    color = androidx.compose.ui.graphics.Color(0xFF8E8E93), fontSize = 14.sp)
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        pendingGroupCode = null; pendingGroupName = null
                        coroutineScope.launch {
                            when (viewModel.joinGroup(capturedGroupCode)) {
                                is ApiResult.Success  -> {
                                    shellNavController.navigate(Screen.Groups.route) {
                                        popUpTo(shellNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    snackbarHostState.showSnackbar("Joined $capturedGroupName!")
                                }
                                is ApiResult.Conflict -> snackbarHostState.showSnackbar("You're already in $capturedGroupName")
                                is ApiResult.NotFound -> snackbarHostState.showSnackbar("Invalid invite code")
                                else                  -> snackbarHostState.showSnackbar("Something went wrong")
                            }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF34C759)),
                ) { Text("Join group", color = androidx.compose.ui.graphics.Color.Black) }
            },
            dismissButton = {
                TextButton(onClick = { pendingGroupCode = null; pendingGroupName = null }) {
                    Text("Cancel", color = androidx.compose.ui.graphics.Color(0xFF8E8E93))
                }
            },
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) Box {
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
                    onNavigateToJoinGroup = {
                        shellNavController.navigate(Screen.JoinGroup.route())
                    },
                    onNavigateToImport = {
                        shellNavController.navigate(Screen.ImportSplitwise.route)
                    },
                    onNavigateToAddExpense = {
                        shellNavController.navigate(Screen.AddExpense.route())
                    },
                )
            }
            composable(Screen.Friends.route) {
                FriendsScreen(
                    onNavigateToAddFriendByEmail = { shellNavController.navigate(Screen.AddFriendByEmail.route) },
                    onNavigateToScanQr = { shellNavController.navigate(Screen.ScanQrCode.route) },
                    onNavigateToImport = { shellNavController.navigate(Screen.ImportSplitwise.route) },
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
                    onNavigateToSettlement = { settlementId ->
                        shellNavController.navigate(Screen.SettlementDetail.route(settlementId))
                    },
                    onNotMember = { groupName ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("You're no longer a member of $groupName")
                        }
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
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
                GroupDetailScreen(
                    onBack = { shellNavController.popBackStack() },
                    onNavigateToSettings = { gId ->
                        shellNavController.navigate(Screen.GroupSettings.route(gId))
                    },
                    onNavigateToSearch = { gId ->
                        shellNavController.navigate(Screen.Search.route(gId))
                    },
                    onNavigateToExpense = { expenseId ->
                        shellNavController.navigate(Screen.ExpenseDetail.route(expenseId))
                    },
                    onNavigateToAddExpense = { gId ->
                        shellNavController.navigate(Screen.AddExpense.route(gId))
                    },
                    onNavigateToAddMember = { gId ->
                        shellNavController.navigate(Screen.AddMember.route(gId))
                    },
                    onNavigateToSettle = { otherUserId, payerId, payerName, currency ->
                        shellNavController.navigate(Screen.SettleUp.route(otherUserId, groupId, payerId, payerName, currency))
                    },
                    onNavigateToSettlement = { settlementId ->
                        shellNavController.navigate(Screen.SettlementDetail.route(settlementId))
                    },
                    onNavigateToBalances = {
                        shellNavController.navigate(Screen.GroupBalances.route(groupId))
                    },
                    onNavigateToAnalytics = {
                        shellNavController.navigate(Screen.GroupAnalytics.route(groupId))
                    },
                )
            }
            composable(
                route = Screen.GroupSettings.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                val groupSettingsVm = hiltViewModel<GroupSettingsViewModel>()
                val defaultCurrency by groupSettingsVm.defaultCurrency.collectAsState()
                GroupSettingsScreen(
                    onBack = { shellNavController.popBackStack() },
                    defaultCurrency = defaultCurrency,
                    onNavigateToAddMember = { gId ->
                        shellNavController.navigate(Screen.AddMember.route(gId))
                    },
                    onGroupDeleted = {
                        shellNavController.popBackStack(Screen.Groups.route, false)
                    },
                    onNavigateToMembers = { gId ->
                        shellNavController.navigate(Screen.GroupMembers.route(gId))
                    },
                    onNavigateToAnalytics = { gId ->
                        shellNavController.navigate(Screen.GroupAnalytics.route(gId))
                    },
                    onNavigateToRecurring = { gId ->
                        shellNavController.navigate(Screen.RecurringExpenses.route(gId))
                    },
                    onNavigateToReminders = { gId ->
                        shellNavController.navigate(Screen.Reminders.route(gId))
                    },
                    onNavigateToInvite = { gId ->
                        shellNavController.navigate(Screen.GroupInvite.route(gId))
                    },
                    onNavigateToCurrency = { _ ->
                        shellNavController.navigate(Screen.CurrencySelect.route)
                    },
                )
            }

            composable(
                route = Screen.GroupMembers.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                GroupMembersScreen(onBack = { shellNavController.popBackStack() })
            }

            composable(
                route = Screen.AddMember.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                AddMemberScreen(
                    onBack = { shellNavController.popBackStack() },
                )
            }

            composable(
                route = Screen.WhoOwesWho.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Who Owes Who") }

            composable(
                route = Screen.GroupBalances.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupBalancesScreen(
                    onBack       = { shellNavController.popBackStack() },
                    onSettleWith = { otherUserId ->
                        shellNavController.navigate(Screen.SettleUp.route(otherUserId, groupId))
                    },
                )
            }

            composable(
                route     = Screen.GroupInvite.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            ) {
                GroupInviteScreen(onBack = { shellNavController.popBackStack() })
            }

            composable(
                route = Screen.TotalsSheet.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { PlaceholderScreen("Totals Sheet") }

            composable(
                route = Screen.GroupAnalytics.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                GroupAnalyticsScreen(onBack = { shellNavController.popBackStack() })
            }

            composable(
                route     = Screen.CreateGroup.route,
                arguments = listOf(navArgument("friendId") {
                    type     = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { backStackEntry ->
                val preselectFriendId = backStackEntry.arguments?.getString("friendId")
                CreateGroupScreen(
                    onBack = { shellNavController.popBackStack() },
                    onGroupCreated = { groupId: String ->
                        shellNavController.navigate(Screen.GroupDetail.route(groupId)) {
                            popUpTo(Screen.CreateGroup.route) { inclusive = true }
                        }
                    },
                    preselectFriendId = preselectFriendId,
                )
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
            ) {
                JoinGroupScreen(
                    onBack    = { shellNavController.popBackStack() },
                    onSuccess = {
                        // Navigate to Groups tab — the new group appears via auto-refresh
                        shellNavController.navigate(Screen.Groups.route) {
                            popUpTo(Screen.Groups.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                route = Screen.RecurringExpenses.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                RecurringExpensesScreen(
                    onBack                  = { shellNavController.popBackStack() },
                    onNavigateToEditExpense = { expenseId ->
                        shellNavController.navigate(Screen.EditExpense.route(expenseId))
                    },
                )
            }

            composable(
                route = Screen.Reminders.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                RemindersScreen(
                    onBack             = { shellNavController.popBackStack() },
                    onNavigateToCreate = { shellNavController.navigate(Screen.CreateReminder.route(groupId)) },
                )
            }

            composable(
                route = Screen.CreateReminder.route,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) {
                CreateReminderScreen(
                    onBack    = { shellNavController.popBackStack() },
                    onCreated = { shellNavController.popBackStack() },
                )
            }

            // ── Expense screens ───────────────────────────────────────────────
            composable(
                route = Screen.AddExpense.route,
                arguments = listOf(
                    navArgument("groupId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("friendId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                )
            ) {
                AddExpenseScreen(
                    onBack = { shellNavController.popBackStack() },
                    onSuccess = { shellNavController.popBackStack() },
                    onNavigateToCurrency = {
                        shellNavController.navigate(Screen.CurrencySelect.route)
                    },
                    onNavigateToItemize = { receiptId ->
                        shellNavController.navigate(Screen.ItemAssignment.route(receiptId))
                    },
                )
            }
            composable(Screen.CurrencySelect.route) { backStackEntry ->
                // Check if we came from AddExpense, EditExpense, or GroupSettings
                val fromAddExpense = remember(backStackEntry) {
                    try {
                        shellNavController.getBackStackEntry(Screen.AddExpense.route)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                val fromEditExpense = remember(backStackEntry) {
                    try {
                        shellNavController.getBackStackEntry(Screen.EditExpense.route)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                val fromGroupSettings = remember(backStackEntry) {
                    try {
                        shellNavController.getBackStackEntry(Screen.GroupSettings.route)
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
                } else if (fromEditExpense) {
                    val parentEntry = remember(backStackEntry) {
                        shellNavController.getBackStackEntry(Screen.EditExpense.route)
                    }
                    val editExpenseViewModel = hiltViewModel<EditExpenseViewModel>(parentEntry)
                    val currency by editExpenseViewModel.currency.collectAsState()
                    CurrencySelectScreen(
                        currentCurrency = currency,
                        onSelect = { selected -> editExpenseViewModel.onCurrencyChanged(selected) },
                        onBack = { shellNavController.popBackStack() },
                    )
                } else if (fromGroupSettings) {
                    val parentEntry = remember(backStackEntry) {
                        shellNavController.getBackStackEntry(Screen.GroupSettings.route)
                    }
                    val groupSettingsViewModel = hiltViewModel<GroupSettingsViewModel>(parentEntry)
                    val currentCurrency by groupSettingsViewModel.defaultCurrency.collectAsState()
                    CurrencySelectScreen(
                        currentCurrency = currentCurrency,
                        onSelect = { selected ->
                            groupSettingsViewModel.saveDefaultCurrency(selected)
                            shellNavController.popBackStack()
                        },
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
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
                EditExpenseScreen(
                    onBack = { shellNavController.popBackStack() },
                    onSuccess = { shellNavController.popBackStack() },
                    onNavigateToCurrency = {
                        shellNavController.navigate(Screen.CurrencySelect.route)
                    },
                    onNavigateToEditItems = {
                        shellNavController.navigate(Screen.EditItemAssignment.route(expenseId))
                    },
                )
            }

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
                arguments = listOf(navArgument("receiptId") { type = NavType.StringType })
            ) { backStackEntry ->
                val receiptId = backStackEntry.arguments?.getString("receiptId") ?: return@composable
                val parentEntry = remember(backStackEntry) {
                    shellNavController.getBackStackEntry(Screen.AddExpense.route)
                }
                val addExpenseViewModel  = hiltViewModel<AddExpenseViewModel>(parentEntry)
                val itemAssignViewModel  = hiltViewModel<ItemAssignmentViewModel>()
                val members by addExpenseViewModel.members.collectAsState()
                val currency by addExpenseViewModel.currency.collectAsState()
                ItemAssignmentScreen(
                    receiptId = receiptId,
                    members   = members,
                    currency  = currency,
                    onBack               = { shellNavController.popBackStack() },
                    onDone               = { assignments ->
                        val rawSplitData = itemAssignViewModel.buildSplitData()

                        // ✅ Distribute tax + fees proportionally before sending to backend.
                        // buildSplitData() returns raw item prices only. The grand total
                        // (receipt.totalAmount) includes tax/fees. Each person pays
                        // tax/fees proportional to their item share.
                        // We round to 2dp and fix any residual on the last entry so the
                        // sum equals receipt.totalAmount exactly — avoiding backend rejection.
                        val receipt = (addExpenseViewModel.receiptState.value
                                as? com.prathik.fairshare.ui.expense.ReceiptScanState.Success)?.receipt
                        val adjustedSplitData: Map<String, Double> = if (receipt != null) {
                            val itemSubtotal = rawSplitData.values.sum()
                            val extraTotal   = receipt.totalAmount - itemSubtotal
                            if (extraTotal != 0.0 && itemSubtotal > 0.0) {
                                val entries = rawSplitData.entries.toList()
                                val rounded = entries.map { (uid, itemAmount) ->
                                    val proportion = itemAmount / itemSubtotal
                                    val adjusted   = itemAmount + (extraTotal * proportion)
                                    uid to (Math.round(adjusted * 100) / 100.0)
                                }.toMutableList()
                                // Fix rounding residual on last entry
                                val roundedSum = rounded.sumOf { it.second }
                                val residual   = Math.round((receipt.totalAmount - roundedSum) * 100) / 100.0
                                if (residual != 0.0 && rounded.isNotEmpty()) {
                                    val last = rounded.last()
                                    rounded[rounded.lastIndex] = last.first to (last.second + residual)
                                }
                                rounded.toMap()
                            } else rawSplitData
                        } else rawSplitData

                        addExpenseViewModel.setItemAssignments(
                            assignments = assignments,
                            splitData   = adjustedSplitData,
                        )
                    },
                    onNavigateToReview   = {
                        shellNavController.navigate(Screen.ReviewSubmit.route)
                    },
                )
            }


            composable(route = Screen.ReviewSubmit.route) { backStackEntry ->
                val addExpenseEntry = remember(backStackEntry) {
                    shellNavController.getBackStackEntry(Screen.AddExpense.route)
                }
                val itemAssignEntry = remember(backStackEntry) {
                    shellNavController.getBackStackEntry(Screen.ItemAssignment.route)
                }
                val addExpenseViewModel = hiltViewModel<AddExpenseViewModel>(addExpenseEntry)
                val itemAssignViewModel = hiltViewModel<ItemAssignmentViewModel>(itemAssignEntry)

                val items       by itemAssignViewModel.items.collectAsState()
                val members     by addExpenseViewModel.members.collectAsState()
                val currency    by addExpenseViewModel.currency.collectAsState()
                val uiState     by addExpenseViewModel.uiState.collectAsState()
                val receiptState by addExpenseViewModel.receiptState.collectAsState()

                val receipt = (receiptState as? com.prathik.fairshare.ui.expense.ReceiptScanState.Success)?.receipt

                // Only pop back if not already navigating away on success.
                // Without this guard, the ViewModel being destroyed resets receiptState
                // to null, triggering a second popBackStack that removes GroupDetail too.
                if (receipt == null &&
                    uiState !is com.prathik.fairshare.ui.expense.AddExpenseUiState.Success &&
                    uiState !is com.prathik.fairshare.ui.expense.AddExpenseUiState.Loading) {
                    shellNavController.popBackStack()
                    return@composable
                }

                if (receipt == null) return@composable

                ReviewSubmitScreen(
                    receipt    = receipt,
                    items      = items,
                    splitData  = itemAssignViewModel.buildSplitData(),
                    members    = members,
                    currency   = currency,
                    isLoading  = uiState is com.prathik.fairshare.ui.expense.AddExpenseUiState.Loading,
                    onBack     = { shellNavController.popBackStack() },
                    onSubmit   = { addExpenseViewModel.submit() },
                )

                // Navigate away on success
                androidx.compose.runtime.LaunchedEffect(uiState) {
                    if (uiState is com.prathik.fairshare.ui.expense.AddExpenseUiState.Success) {
                        shellNavController.popBackStack(Screen.AddExpense.route, inclusive = true)
                    }
                }
            }
            composable(
                route = Screen.EditItemAssignment.route,
                arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
            ) { backStackEntry ->
                val expenseId           = backStackEntry.arguments?.getString("expenseId") ?: return@composable
                val itemAssignViewModel  = hiltViewModel<ItemAssignmentViewModel>()
                // EditExpense is on the backstack — share its ViewModel for members/currency
                val editExpenseEntry    = remember(backStackEntry) {
                    shellNavController.getBackStackEntry(Screen.EditExpense.route)
                }
                val editExpenseVm       = hiltViewModel<EditExpenseViewModel>(editExpenseEntry)
                val members  by editExpenseVm.members.collectAsState()
                val currency by editExpenseVm.currency.collectAsState()

                androidx.compose.runtime.LaunchedEffect(expenseId) {
                    itemAssignViewModel.loadItemsForEdit(expenseId)
                }

                ItemAssignmentScreen(
                    receiptId          = "",
                    members            = members,
                    currency           = currency,
                    onBack             = { shellNavController.popBackStack() },
                    onDone             = { assignments ->
                        val rawSplitData  = itemAssignViewModel.buildSplitData()
                        val itemSubtotal  = rawSplitData.values.sum()
                        val expenseTotal  = editExpenseVm.amount.value.toDoubleOrNull() ?: itemSubtotal
                        val extraTotal    = expenseTotal - itemSubtotal

                        // Distribute tax/fees proportionally — same logic as the add flow
                        val adjustedSplitData: Map<String, Double> =
                            if (extraTotal != 0.0 && itemSubtotal > 0.0) {
                                val entries = rawSplitData.entries.toList()
                                val rounded = entries.map { (uid, itemAmount) ->
                                    val proportion = itemAmount / itemSubtotal
                                    val adjusted   = itemAmount + (extraTotal * proportion)
                                    uid to (Math.round(adjusted * 100) / 100.0)
                                }.toMutableList()
                                // Fix any rounding residual on the last entry so sum == expenseTotal exactly
                                val roundedSum = rounded.sumOf { it.second }
                                val residual   = Math.round((expenseTotal - roundedSum) * 100) / 100.0
                                if (residual != 0.0 && rounded.isNotEmpty()) {
                                    val last = rounded.last()
                                    rounded[rounded.lastIndex] = last.first to (last.second + residual)
                                }
                                rounded.toMap()
                            } else rawSplitData

                        editExpenseVm.setItemAssignments(assignments, adjustedSplitData)
                    },
                    onNavigateToReview = {
                        shellNavController.navigate(Screen.EditReviewSubmit.route)
                    },
                )
            }

            // ── Edit review + submit (edit itemized flow) ─────────────────────────
            composable(route = Screen.EditReviewSubmit.route) { backStackEntry ->
                val editExpenseEntry    = remember(backStackEntry) {
                    shellNavController.getBackStackEntry(Screen.EditExpense.route)
                }
                val itemAssignEntry     = remember(backStackEntry) {
                    shellNavController.getBackStackEntry(Screen.EditItemAssignment.route)
                }
                val editExpenseVm       = hiltViewModel<EditExpenseViewModel>(editExpenseEntry)
                val itemAssignViewModel  = hiltViewModel<ItemAssignmentViewModel>(itemAssignEntry)

                val items    by itemAssignViewModel.items.collectAsState()
                val members  by editExpenseVm.members.collectAsState()
                val currency by editExpenseVm.currency.collectAsState()
                val saveState by editExpenseVm.saveState.collectAsState()
                val amountStr by editExpenseVm.amount.collectAsState()
                val amount = amountStr.toDoubleOrNull() ?: 0.0

                // Synthetic receipt so ReviewSubmitScreen can display totals
                val syntheticReceipt = com.prathik.fairshare.domain.model.Receipt(
                    id              = "",
                    expenseId       = null,
                    scannedById     = "",
                    imageUrl        = null,
                    merchantName    = null,
                    merchantAddress = null,
                    subtotal        = amount,
                    taxAmount       = null,
                    tipAmount       = null,
                    totalAmount     = amount,
                    currency        = currency,
                    paymentMethod   = null,
                    receiptDate     = null,
                    scanConfidence  = null,
                    itemCount       = items.size,
                    createdAt       = "",
                )

                val expenseId = itemAssignEntry.arguments?.getString("expenseId") ?: ""

                androidx.compose.runtime.LaunchedEffect(saveState) {
                    if (saveState is EditSaveState.Success) {
                        // Save item assignments (who owns which items) then pop
                        itemAssignViewModel.saveAssignments(expenseId)
                        editExpenseVm.resetSaveState()
                        shellNavController.popBackStack(Screen.EditExpense.route, inclusive = true)
                    }
                }

                ReviewSubmitScreen(
                    receipt   = syntheticReceipt,
                    items     = items,
                    splitData = itemAssignViewModel.buildSplitData(),
                    members   = members,
                    currency  = currency,
                    isLoading = saveState is EditSaveState.Loading,
                    onBack    = { shellNavController.popBackStack() },
                    onSubmit  = { editExpenseVm.save() },
                )
            }

            // ── Settlement screens ────────────────────────────────────────────
            composable(
                route = Screen.SettleUp.route,
                arguments = listOf(
                    navArgument("otherUserId") { type = NavType.StringType },
                    navArgument("groupId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("payerId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("currency") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
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

            composable(
                route = Screen.SettlementDetail.route,
                arguments = listOf(navArgument("settlementId") { type = NavType.StringType }),
            ) {
                SettlementDetailScreen(
                    onBack           = { shellNavController.popBackStack() },
                    onNavigateToEdit = { settlementId ->
                        shellNavController.navigate(Screen.EditSettlement.route(settlementId))
                    },
                    onDeleted        = { shellNavController.popBackStack() },
                )
            }

            composable(
                route = Screen.EditSettlement.route,
                arguments = listOf(navArgument("settlementId") { type = NavType.StringType }),
            ) {
                EditSettlementScreen(
                    onBack  = { shellNavController.popBackStack() },
                    onSaved = { shellNavController.popBackStack() },
                )
            }

            // ── Friend screens ────────────────────────────────────────────────
            composable(
                route = Screen.FriendDetail.route,
                arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            ) { backStackEntry ->
                val friendId = backStackEntry.arguments?.getString("friendId") ?: return@composable
                FriendDetailScreen(
                    onBack = { shellNavController.popBackStack() },
                    onNavigateToSettings = {
                        shellNavController.navigate(
                            Screen.FriendSettings.route(
                                friendId
                            )
                        )
                    },
                    onNavigateToExpense = { expenseId ->
                        shellNavController.navigate(
                            Screen.ExpenseDetail.route(
                                expenseId
                            )
                        )
                    },
                    onNavigateToAddExpense = { shellNavController.navigate(Screen.AddExpense.route(friendId = friendId)) },
                    onNavigateToSettle = { fId, gId, payerId, payerName ->
                        shellNavController.navigate(Screen.SettleUp.route(fId, gId, payerId, payerName))
                    },
                    onNavigateToSearch = {
                        shellNavController.navigate(Screen.Search.route())
                    },
                    onNavigateToSettlement = { settlementId ->
                        shellNavController.navigate(Screen.SettlementDetail.route(settlementId))
                    },
                    onNavigateToGroup = { groupId ->
                        shellNavController.navigate(Screen.GroupDetail.route(groupId))
                    },
                    onNavigateToGroupsTab = {
                        shellNavController.navigate(Screen.Groups.route) {
                            popUpTo(Screen.Groups.route) { inclusive = false }
                        }
                    },
                    onNavigateToAnalytics = {
                        shellNavController.navigate(Screen.FriendAnalytics.route(friendId))
                    },
                    onNavigateToRealFriend = { realFriendId ->
                        // Pop the placeholder's FriendDetail off the back stack and
                        // navigate to the real friend's screen so back takes user to Friends tab
                        shellNavController.navigate(Screen.FriendDetail.route(realFriendId)) {
                            popUpTo(Screen.FriendDetail.route(friendId)) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                route = Screen.FriendSettings.route,
                arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            ) { backStackEntry ->
                val friendId = backStackEntry.arguments?.getString("friendId") ?: return@composable
                FriendSettingsScreen(
                    onBack    = { shellNavController.popBackStack() },
                    onRemoved = { shellNavController.popBackStack(Screen.Friends.route, false) },
                    onNavigateToSettleUp    = {
                        shellNavController.navigate(Screen.SettleUp.route(friendId))
                    },
                    onNavigateToGroup       = { groupId ->
                        shellNavController.navigate(Screen.GroupDetail.route(groupId))
                    },
                    onNavigateToCreateGroup = {
                        shellNavController.navigate(Screen.CreateGroup.route(friendId))
                    },
                )
            }

            composable(
                route = Screen.FriendAnalytics.route,
                arguments = listOf(navArgument("friendId") { type = NavType.StringType })
            ) {
                FriendAnalyticsScreen(onBack = { shellNavController.popBackStack() })
            }

            composable(Screen.AddFriend.route) {
                AddFriendScreen(
                    onBack = { shellNavController.popBackStack() },
                    onDone = { shellNavController.popBackStack(Screen.Friends.route, false) },
                )
            }

            composable(Screen.AddFriendByEmail.route) {
                AddFriendScreen(
                    onBack = { shellNavController.popBackStack() },
                    onDone = { shellNavController.popBackStack(Screen.Friends.route, false) },
                )
            }

            composable(Screen.QrCode.route) {
                QrCodeScreen(
                    onBack = { shellNavController.popBackStack() },
                    onCodeScanned = { code ->
                        if (code.startsWith("FAIR-", ignoreCase = true)) {
                            coroutineScope.launch {
                                when (val result = viewModel.lookupByFriendCode(code)) {
                                    is ApiResult.Success  -> { pendingFriendCode = code; pendingFriend = result.data }
                                    is ApiResult.NotFound -> snackbarHostState.showSnackbar("Invalid friend code")
                                    else                  -> snackbarHostState.showSnackbar("Could not look up this code")
                                }
                            }
                        } else {
                            coroutineScope.launch {
                                when (val result = viewModel.previewGroup(code)) {
                                    is ApiResult.Success  -> { pendingGroupCode = code; pendingGroupName = result.data.name }
                                    is ApiResult.NotFound -> snackbarHostState.showSnackbar("Invalid invite code")
                                    else                  -> snackbarHostState.showSnackbar("Could not look up this group")
                                }
                            }
                        }
                    },
                )
            }

            composable(Screen.ScanQrCode.route) {
                QrCodeScreen(
                    onBack = { shellNavController.popBackStack() },
                    onCodeScanned = { code ->
                        shellNavController.popBackStack()
                        if (code.startsWith("FAIR-", ignoreCase = true)) {
                            // Friend code — lookup name then show confirm dialog
                            coroutineScope.launch {
                                when (val result = viewModel.lookupByFriendCode(code)) {
                                    is ApiResult.Success  -> { pendingFriendCode = code; pendingFriend = result.data }
                                    is ApiResult.NotFound -> snackbarHostState.showSnackbar("Invalid friend code")
                                    else                  -> snackbarHostState.showSnackbar("Could not look up this code")
                                }
                            }
                        } else {
                            // Group invite code — preview group then show confirm dialog
                            coroutineScope.launch {
                                when (val result = viewModel.previewGroup(code)) {
                                    is ApiResult.Success  -> { pendingGroupCode = code; pendingGroupName = result.data.name }
                                    is ApiResult.NotFound -> snackbarHostState.showSnackbar("Invalid invite code")
                                    else                  -> snackbarHostState.showSnackbar("Could not look up this group")
                                }
                            }
                        }
                    },
                )
            }


            // ── Account screens ───────────────────────────────────────────────
            composable(Screen.EditProfile.route) {
                EditProfileScreen(
                    onBack               = { shellNavController.popBackStack() },
                    onNavigateToPassword = { shellNavController.navigate(Screen.ChangePassword.route) },
                )
            }
            composable(Screen.ChangePassword.route) {
                ChangePasswordScreen(
                    onBack      = { shellNavController.popBackStack() },
                    onLoggedOut = {
                        rootNavController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                route = Screen.ConfirmEmailChange.route,
                arguments = listOf(
                    androidx.navigation.navArgument("token") {
                        type         = androidx.navigation.NavType.StringType
                        nullable     = true
                        defaultValue = null
                    }
                ),
            ) { backStackEntry ->
                val tokenFromNav = backStackEntry.arguments?.getString("token")
                val token = emailChangeToken ?: tokenFromNav
                com.prathik.fairshare.ui.account.ConfirmEmailChangeScreen(
                    token  = token,
                    onDone = {
                        shellNavController.navigate(Screen.Groups.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.MyAnalytics.route) {
                MyAnalyticsScreen(onBack = { shellNavController.popBackStack() })
            }
            composable(Screen.ImportSplitwise.route) {
                com.prathik.fairshare.ui.account.ImportSplitwiseScreen(
                    onBack          = { shellNavController.popBackStack() },
                    onGroupImported = { groupId ->
                        shellNavController.navigate(Screen.GroupDetail.route(groupId)) {
                            popUpTo(Screen.ImportSplitwise.route) { inclusive = true }
                        }
                    },
                )
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