package com.prathik.fairshare.domain.usecase.balance

import com.prathik.fairshare.domain.model.Expense
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure, stateless calculator that projects a single group expense's financial
 * impact into per-friend deltas from the current user's perspective.
 *
 * This is the missing piece for Wave 2F: group expense pending changes must
 * update FriendDetail and FriendsHome just like they update GroupDetail.
 *
 * Calculation rule (pairwise):
 *   For each other participant X:
 *     currentUserPaidForX  = payerData[currentUser] / totalPaid * splitData[X]
 *     xPaidForCurrentUser  = payerData[X] / totalPaid * splitData[currentUser]
 *     delta[X]             = currentUserPaidForX - xPaidForCurrentUser
 *
 * Positive delta → friend owes current user more.
 * Negative delta → current user owes friend more.
 *
 * Special cases:
 *   - Single payer = current user: delta[X] = splitData[X] for every other X.
 *   - Single payer = someone else: delta[payer] -= splitData[currentUser].
 *   - Multi-payer: proportional allocation as above.
 *
 * Zero deltas are omitted from the result map.
 */
@Singleton
class PendingFriendImpactCalculator @Inject constructor() {

    /**
     * Calculate per-friend deltas for one expense operation.
     *
     * @param payers        Expense.PayerDetail list (who paid how much).
     * @param splits        Expense.SplitDetail list (who owes how much).
     * @param currentUserId The logged-in user's ID.
     * @return Map of friendId → delta (positive = friend owes you, negative = you owe friend).
     */
    fun calculate(
        payers        : List<Expense.PayerDetail>,
        splits        : List<Expense.SplitDetail>,
        currentUserId : String,
    ): Map<String, Double> {
        if (payers.isEmpty() || splits.isEmpty()) return emptyMap()

        val payerMap  = payers.associate { it.userId to it.amountPaid }
        val splitMap  = splits.associate { it.userId to it.amountOwed }
        val totalPaid = payerMap.values.sumOf { it }.takeIf { it > 0 } ?: return emptyMap()

        val currentUserPaid  = payerMap[currentUserId] ?: 0.0
        val currentUserOwes  = splitMap[currentUserId] ?: 0.0

        val result = mutableMapOf<String, Double>()

        // Other participants in either payer or split list.
        val otherUserIds = (payers.map { it.userId } + splits.map { it.userId })
            .toSet()
            .filter { it != currentUserId }

        for (friendId in otherUserIds) {
            val friendPaid  = payerMap[friendId] ?: 0.0
            val friendOwes  = splitMap[friendId] ?: 0.0

            // What the current user effectively paid toward friend's share.
            val currentUserPaidForFriend =
                (currentUserPaid / totalPaid) * friendOwes

            // What the friend effectively paid toward current user's share.
            val friendPaidForCurrentUser =
                (friendPaid / totalPaid) * currentUserOwes

            val delta = currentUserPaidForFriend - friendPaidForCurrentUser

            if (kotlin.math.abs(delta) > 0.001) {   // ignore floating-point dust
                result[friendId] = delta
            }
        }
        return result
    }
}