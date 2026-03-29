package com.prathik.fairshare.ui.navigation

/**
 * Defines all 39 navigation routes in one place.
 *
 * Every screen is a sealed object — no hardcoded route strings anywhere else.
 * Screens with arguments define a route() function for type-safe navigation.
 *
 * Usage:
 *   navController.navigate(Screen.GroupDetail.route(groupId))
 *   navController.navigate(Screen.Login.route)
 */
sealed class Screen(val route: String) {

    // ── Auth ──────────────────────────────────────────────────────────────────
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object VerifyEmail : Screen("verify_email?email={email}") {
        fun route(email: String) = "verify_email?email=$email"
    }
    object ForgotPassword : Screen("forgot_password")

    // ── Main tabs ─────────────────────────────────────────────────────────────
    object Groups : Screen("groups")
    object Friends : Screen("friends")
    object Activity : Screen("activity")
    object Account : Screen("account")

    // ── Group ─────────────────────────────────────────────────────────────────
    object GroupDetail : Screen("group/{groupId}") {
        fun route(groupId: String) = "group/$groupId"
    }

    object GroupSettings : Screen("group/{groupId}/settings") {
        fun route(groupId: String) = "group/$groupId/settings"
    }

    object GroupMembers : Screen("group/{groupId}/members") {
        fun route(groupId: String) = "group/$groupId/members"
    }

    object AddMember : Screen("group/{groupId}/add_member") {
        fun route(groupId: String) = "group/$groupId/add_member"
    }

    object WhoOwesWho : Screen("group/{groupId}/who_owes_who") {
        fun route(groupId: String) = "group/$groupId/who_owes_who"
    }

    object TotalsSheet : Screen("group/{groupId}/totals") {
        fun route(groupId: String) = "group/$groupId/totals"
    }

    object GroupAnalytics : Screen("group/{groupId}/analytics") {
        fun route(groupId: String) = "group/$groupId/analytics"
    }

    object CreateGroup : Screen("create_group")
    object JoinGroup : Screen("join_group?inviteCode={inviteCode}") {
        fun route(inviteCode: String? = null) =
            if (inviteCode != null) "join_group?inviteCode=$inviteCode"
            else "join_group"
    }

    // ── Expense ───────────────────────────────────────────────────────────────
    object AddExpense : Screen("add_expense?groupId={groupId}") {
        fun route(groupId: String? = null) =
            if (groupId != null) "add_expense?groupId=$groupId"
            else "add_expense?groupId="  // ← always include the param
    }

    object EditExpense : Screen("expense/{expenseId}/edit") {
        fun route(expenseId: String) = "expense/$expenseId/edit"
    }

    object ExpenseDetail : Screen("expense/{expenseId}") {
        fun route(expenseId: String) = "expense/$expenseId"
    }

    object ReceiptScan : Screen("receipt_scan/{expenseId}") {
        fun route(expenseId: String) = "receipt_scan/$expenseId"
    }

    object ItemAssignment : Screen("expense/{expenseId}/items") {
        fun route(expenseId: String) = "expense/$expenseId/items"
    }

    // ── Settlement ────────────────────────────────────────────────────────────
    object SettleUp : Screen("settle/{otherUserId}?groupId={groupId}") {
        fun route(otherUserId: String, groupId: String? = null) =
            if (groupId != null) "settle/$otherUserId?groupId=$groupId"
            else "settle/$otherUserId"
    }

    object PartialSettle : Screen("settle/{otherUserId}/partial?groupId={groupId}") {
        fun route(otherUserId: String, groupId: String? = null) =
            if (groupId != null) "settle/$otherUserId/partial?groupId=$groupId"
            else "settle/$otherUserId/partial"
    }

    object SettlementHistory : Screen("settle/{otherUserId}/history") {
        fun route(otherUserId: String) = "settle/$otherUserId/history"
    }

    // ── Friend ────────────────────────────────────────────────────────────────
    object FriendDetail : Screen("friend/{friendId}") {
        fun route(friendId: String) = "friend/$friendId"
    }

    object FriendSettings : Screen("friend/{friendId}/settings") {
        fun route(friendId: String) = "friend/$friendId/settings"
    }

    object FriendAnalytics : Screen("friend/{friendId}/analytics") {
        fun route(friendId: String) = "friend/$friendId/analytics"
    }

    object AddFriend : Screen("add_friend")
    object FriendRequests : Screen("friend_requests")

    // ── Account ───────────────────────────────────────────────────────────────
    object EditProfile : Screen("edit_profile")
    object ChangePassword : Screen("change_password")
    object MyAnalytics : Screen("my_analytics")
    object ImportSplitwise : Screen("import_splitwise")

    object CurrencySelect : Screen("currency_select")

    // ── Search ────────────────────────────────────────────────────────────────
    object Search : Screen("search")

    // ── Recurring + Reminders ─────────────────────────────────────────────────
    object RecurringExpenses : Screen("group/{groupId}/recurring") {
        fun route(groupId: String) = "group/$groupId/recurring"
    }

    object Reminders : Screen("group/{groupId}/reminders") {
        fun route(groupId: String) = "group/$groupId/reminders"
    }

    object CreateReminder : Screen("group/{groupId}/reminders/create") {
        fun route(groupId: String) = "group/$groupId/reminders/create"
    }
}