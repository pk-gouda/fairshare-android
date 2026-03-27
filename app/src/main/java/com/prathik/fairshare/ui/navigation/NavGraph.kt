package com.prathik.fairshare.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink

/**
 * Wires all 39 screens into the navigation graph.
 *
 * Each screen has a placeholder composable for now.
 * Screens are replaced with real implementations as we build them
 * Day 10 onwards.
 *
 * Start destination is Splash — it checks IsLoggedInUseCase
 * and navigates to either Login or Groups.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController    = navController,
        startDestination = Screen.Splash.route,
        modifier         = modifier,
    ) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Splash.route) {
            PlaceholderScreen("Splash")
        }
        composable(Screen.Login.route) {
            PlaceholderScreen("Login")
        }
        composable(Screen.Register.route) {
            PlaceholderScreen("Register")
        }
        composable(
            route = Screen.VerifyEmail.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            PlaceholderScreen("Verify Email — ${backStackEntry.arguments?.getString("email")}")
        }
        composable(Screen.ForgotPassword.route) {
            PlaceholderScreen("Forgot Password")
        }

        // ── Main tabs ─────────────────────────────────────────────────────────
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

        // ── Group ─────────────────────────────────────────────────────────────
        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Group Detail")
        }
        composable(
            route = Screen.GroupSettings.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Group Settings")
        }
        composable(
            route = Screen.GroupMembers.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Group Members")
        }
        composable(
            route = Screen.AddMember.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Add Member")
        }
        composable(
            route = Screen.WhoOwesWho.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Who Owes Who")
        }
        composable(
            route = Screen.TotalsSheet.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Totals Sheet")
        }
        composable(
            route = Screen.GroupAnalytics.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Group Analytics")
        }
        composable(Screen.CreateGroup.route) {
            PlaceholderScreen("Create Group")
        }
        composable(
            route = Screen.JoinGroup.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "fairshare://join/{inviteCode}" },
                navDeepLink { uriPattern = "https://fairshare.app/join/{inviteCode}" },
            )
        ) {
            PlaceholderScreen("Join Group")
        }

        // ── Expense ───────────────────────────────────────────────────────────
        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(
                navArgument("groupId") {
                    type     = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            PlaceholderScreen("Add Expense")
        }
        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Edit Expense")
        }
        composable(
            route = Screen.ExpenseDetail.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Expense Detail")
        }
        composable(
            route = Screen.ReceiptScan.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Receipt Scan")
        }
        composable(
            route = Screen.ItemAssignment.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Item Assignment")
        }

        // ── Settlement ────────────────────────────────────────────────────────
        composable(
            route = Screen.SettleUp.route,
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("groupId") {
                    type     = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            PlaceholderScreen("Settle Up")
        }
        composable(
            route = Screen.PartialSettle.route,
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("groupId") {
                    type     = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            PlaceholderScreen("Partial Settle")
        }
        composable(
            route = Screen.SettlementHistory.route,
            arguments = listOf(navArgument("otherUserId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Settlement History")
        }

        // ── Friend ────────────────────────────────────────────────────────────
        composable(
            route = Screen.FriendDetail.route,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Friend Detail")
        }
        composable(
            route = Screen.FriendSettings.route,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Friend Settings")
        }
        composable(
            route = Screen.FriendAnalytics.route,
            arguments = listOf(navArgument("friendId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Friend Analytics")
        }
        composable(Screen.AddFriend.route) {
            PlaceholderScreen("Add Friend")
        }

        // ── Account ───────────────────────────────────────────────────────────
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

        // ── Search ────────────────────────────────────────────────────────────
        composable(Screen.Search.route) {
            PlaceholderScreen("Search")
        }

        // ── Recurring + Reminders ─────────────────────────────────────────────
        composable(
            route = Screen.RecurringExpenses.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Recurring Expenses")
        }
        composable(
            route = Screen.Reminders.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Reminders")
        }
        composable(
            route = Screen.CreateReminder.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            PlaceholderScreen("Create Reminder")
        }
    }
}