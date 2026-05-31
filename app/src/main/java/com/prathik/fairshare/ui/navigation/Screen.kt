package com.prathik.fairshare.ui.navigation

import android.net.Uri

/**
 * Defines app navigation routes in one place.
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
        fun route(email: String? = null) =
            if (email != null) "verify_email?email=${Uri.encode(email)}" else "verify_email"
    }

    object ForgotPassword : Screen("forgot_password")

    // ── Main tabs ─────────────────────────────────────────────────────────────
    object Groups : Screen("groups")
    object Friends : Screen("friends")
    object Activity : Screen("activity")
    object Account      : Screen("account")
    object CloseAccount  : Screen("close_account")

    // ── Group ─────────────────────────────────────────────────────────────────
    object GroupDetail : Screen("group/{groupId}") {
        fun route(groupId: String) = "group/$groupId"
    }

    object GroupSettings : Screen("group/{groupId}/settings") {
        fun route(groupId: String) = "group/$groupId/settings"
    }

    object CustomizeGroup : Screen("group/{groupId}/customize") {
        fun route(groupId: String) = "group/$groupId/customize"
    }

    object GroupMembers : Screen("group/{groupId}/members") {
        fun route(groupId: String) = "group/$groupId/members"
    }

    object AddMember : Screen("group/{groupId}/add_member") {
        fun route(groupId: String) = "group/$groupId/add_member"
    }


    object GroupBalances : Screen("group/{groupId}/balances") {
        fun route(groupId: String) = "group/$groupId/balances"
    }


    object GroupInvite : Screen("group/{groupId}/invite") {
        fun route(groupId: String) = "group/$groupId/invite"
    }
    object GroupAnalytics : Screen("group/{groupId}/analytics") {
        fun route(groupId: String) = "group/$groupId/analytics"
    }

    object CreateGroup : Screen("create_group?friendId={friendId}") {
        fun route(friendId: String? = null) =
            if (friendId != null) "create_group?friendId=$friendId" else "create_group"
    }
    object JoinGroup : Screen("join_group?inviteCode={inviteCode}") {
        fun route(inviteCode: String? = null) =
            if (inviteCode != null) "join_group?inviteCode=${Uri.encode(inviteCode)}"
            else "join_group"
    }

    // ── Expense ───────────────────────────────────────────────────────────────
    object AddExpense : Screen("add_expense?groupId={groupId}&friendId={friendId}") {
        fun route(groupId: String? = null, friendId: String? = null) =
            "add_expense?groupId=${groupId ?: ""}&friendId=${friendId ?: ""}"
    }

    object EditExpense : Screen("expense/{expenseId}/edit") {
        fun route(expenseId: String) = "expense/$expenseId/edit"
    }

    object ExpenseDetail : Screen("expense/{expenseId}") {
        fun route(expenseId: String) = "expense/$expenseId"
    }


    object ConfirmItems : Screen("confirm_items/{receiptId}") {
        fun route(receiptId: String) = "confirm_items/$receiptId"
    }

    object ItemAssignment : Screen("item_assignment/{receiptId}") {
        fun route(receiptId: String) = "item_assignment/$receiptId"
    }

    object ReviewSubmit : Screen("review_submit")

    object EditItemAssignment : Screen("edit_items/{expenseId}") {
        fun route(expenseId: String) = "edit_items/$expenseId"
    }

    object EditReviewSubmit : Screen("edit_review_submit")

    // ── Settlement ────────────────────────────────────────────────────────────
    object SettleUp : Screen("settle/{otherUserId}?groupId={groupId}&payerId={payerId}&payerName={payerName}&currency={currency}") {
        fun route(otherUserId: String, groupId: String? = null, payerId: String? = null, payerName: String? = null, currency: String? = null) =
            "settle/$otherUserId?groupId=${groupId ?: ""}&payerId=${payerId ?: ""}" +
                    "&payerName=${Uri.encode(payerName ?: "")}&currency=${Uri.encode(currency ?: "")}"
    }



    object SettlementDetail : Screen("settlement/{settlementId}") {
        fun route(settlementId: String) = "settlement/$settlementId"
    }

    object EditSettlement : Screen("settlement/{settlementId}/edit") {
        fun route(settlementId: String) = "settlement/$settlementId/edit"
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
    object AddFriendByEmail : Screen("add_friend_email")
    object QrCode : Screen("qr_code")
    object ScanQrCode : Screen("scan_qr_code")

    // ── Account ───────────────────────────────────────────────────────────────
    object ConfirmEmailChange : Screen("confirm_email_change?token={token}") {
        fun route(token: String) = "confirm_email_change?token=${Uri.encode(token)}"
    }

    object EditProfile : Screen("edit_profile")
    object ChangePassword : Screen("change_password")
    object MyAnalytics : Screen("my_analytics")
    object ImportSplitwise : Screen("import_splitwise")

    object CurrencySelect : Screen("currency_select")

    // ── Search ────────────────────────────────────────────────────────────────
    object Search : Screen("search?groupId={groupId}") {
        fun route(groupId: String? = null) =
            if (groupId != null) "search?groupId=$groupId" else "search"
    }

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