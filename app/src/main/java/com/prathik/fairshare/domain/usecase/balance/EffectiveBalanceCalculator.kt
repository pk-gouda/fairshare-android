package com.prathik.fairshare.domain.usecase.balance

import com.prathik.fairshare.data.local.PendingBalanceImpactEntity
import com.prathik.fairshare.data.local.PendingOperationEntity
import com.prathik.fairshare.domain.model.BalanceCurrencyEntry
import com.prathik.fairshare.domain.model.Expense
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure, stateless calculator for effective (confirmed + pending) balance overlays.
 *
 * Called by GroupsViewModel and FriendsViewModel whenever EITHER confirmed balance data
 * OR active pending ops change. Both ViewModels call the same methods, guaranteeing that
 * GroupsHome and FriendsHome always show the same global top total.
 *
 * None of these methods hit the network or Room — callers supply all data.
 * That makes them fast enough to call synchronously inside collect{} lambdas.
 *
 * Delta rules:
 *   CREATE_EXPENSE  → +expense.yourBalance
 *   DELETE_EXPENSE  → -expense.yourBalance  (expense may be soft-deleted, so callers
 *                                             must supply it from getCachedExpense which
 *                                             reads deleted rows via getById)
 *   RESTORE_EXPENSE → +expense.yourBalance
 *   UPDATE_EXPENSE  → +PendingBalanceImpactEntity.delta (newYourBalance - oldYourBalance)
 */
@Singleton
class EffectiveBalanceCalculator @Inject constructor() {

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Global effective summary shown in the top bar of both GroupsHome and FriendsHome.
     *
     * @param confirmedEntries  Per-currency entries from ALL_BALANCES confirmed cache.
     * @param ops               All currently active pending expense operations.
     * @param expenseCache      Map resourceId → Expense for all ops (including deleted).
     * @param impacts           Map operationId → PendingBalanceImpactEntity for UPDATE ops.
     *
     * @return null if ops is empty (caller should show confirmed data).
     */
    fun globalEffectiveSummary(
        confirmedEntries : List<BalanceCurrencyEntry>,
        ops              : List<PendingOperationEntity>,
        expenseCache     : Map<String, Expense>,
        impacts          : Map<String, PendingBalanceImpactEntity>,
    ): GlobalEffectiveResult? {
        if (ops.isEmpty()) return null

        val deltasByCurrency = buildDeltasByCurrency(ops, expenseCache, impacts)
        if (deltasByCurrency.isEmpty()) return null

        val newEntries = confirmedEntries.map { entry ->
            val delta  = deltasByCurrency[entry.currency] ?: 0.0
            val newNet = entry.net + delta
            entry.copy(
                owedToMe = if (newNet > 0) newNet else 0.0,
                youOwe   = if (newNet < 0) -newNet else 0.0,
                net      = newNet,
            )
        }
        // Add entries for currencies that don't exist in confirmed data yet.
        val existingCurrencies = confirmedEntries.map { it.currency }.toSet()
        val extraEntries = deltasByCurrency
            .filterKeys { it !in existingCurrencies }
            .map { (cur, d) ->
                BalanceCurrencyEntry(
                    currency = cur,
                    owedToMe = if (d > 0) d else 0.0,
                    youOwe   = if (d < 0) -d else 0.0,
                    net      = d,
                )
            }

        val effectiveEntries = (newEntries + extraEntries)
            .filter { it.owedToMe > 0.0 || it.youOwe > 0.0 }

        return GlobalEffectiveResult(
            owedToMe = effectiveEntries.sumOf { it.owedToMe },
            youOwe   = effectiveEntries.sumOf { it.youOwe },
            entries  = effectiveEntries,
        )
    }

    /**
     * Per-group effective balances for GroupsHome tiles.
     *
     * @param confirmedGroupBase  Map groupId → cached confirmed balance (single currency scalar).
     * @param ops                 All active pending ops.
     * @param expenseCache        Map resourceId → Expense.
     * @param impacts             Map operationId → PendingBalanceImpactEntity.
     *
     * @return Map groupId → (effectiveAmount, currency).
     *         Only contains entries for groups with at least one active pending op.
     */
    fun effectiveGroupBalances(
        confirmedGroupBase: Map<String, Pair<Double, String>>,  // groupId → (amount, currency)
        ops               : List<PendingOperationEntity>,
        expenseCache      : Map<String, Expense>,
        impacts           : Map<String, PendingBalanceImpactEntity>,
    ): Map<String, Pair<Double, String>> {
        if (ops.isEmpty()) return emptyMap()

        // Group ops by their expense's groupId.
        val opsByGroup = mutableMapOf<String, MutableList<PendingOperationEntity>>()
        for (op in ops) {
            val resource = op.localResourceId ?: op.serverResourceId ?: continue
            val expense  = expenseCache[resource] ?: continue
            val gId      = expense.groupId ?: continue
            opsByGroup.getOrPut(gId) { mutableListOf() }.add(op)
        }

        val result = mutableMapOf<String, Pair<Double, String>>()
        for ((gId, groupOps) in opsByGroup) {
            val confirmedPair = confirmedGroupBase[gId]
            // Currency safety — only apply delta when all pending expenses share one currency.
            val currencies = groupOps.mapNotNull { op ->
                val r = op.localResourceId ?: op.serverResourceId ?: return@mapNotNull null
                expenseCache[r]?.currency
            }.toSet()
            if (currencies.size != 1) continue  // mixed pending currencies — skip
            val pendingCurrency = currencies.single()
            // If no confirmed balance yet, use 0.0 base with pending currency.
            // This lets groups with only offline-created expenses show optimistic balance.
            val confirmedBase = confirmedPair?.first ?: 0.0
            val currency = confirmedPair?.second ?: pendingCurrency
            // If confirmed currency differs from pending, skip (mixed context).
            if (confirmedPair != null && currency != pendingCurrency) continue

            var delta = 0.0
            for (op in groupOps) {
                val r       = op.localResourceId ?: op.serverResourceId ?: continue
                val expense = expenseCache[r] ?: continue
                delta += opDelta(op, expense, impacts)
            }
            result[gId] = Pair(confirmedBase + delta, currency)
        }
        return result
    }

    /**
     * Per-friend effective balances for FriendsHome rows.
     *
     * @param confirmedFriendBase  Map friendId → list of (amount, currency) per currency.
     * @param ops                  All active pending ops.
     * @param expenseCache         Map resourceId → Expense.
     * @param impacts              Map operationId → PendingBalanceImpactEntity.
     *
     * @return Map friendId → effective list of (amount, currency).
     *         Only contains friends with at least one active direct pending op.
     */
    fun effectiveFriendBalances(
        confirmedFriendBase: Map<String, List<Pair<Double, String>>>,
        ops                : List<PendingOperationEntity>,
        expenseCache       : Map<String, Expense>,
        impacts            : Map<String, PendingBalanceImpactEntity>,
        otherUserIdForExpense: (String) -> String?,  // resourceId → otherUserId from cache
    ): Map<String, List<Pair<Double, String>>> {
        if (ops.isEmpty()) return emptyMap()

        val opsByFriend = mutableMapOf<String, MutableList<PendingOperationEntity>>()
        for (op in ops) {
            val resource  = op.localResourceId ?: op.serverResourceId ?: continue
            val friendId  = otherUserIdForExpense(resource) ?: continue  // null = group expense
            opsByFriend.getOrPut(friendId) { mutableListOf() }.add(op)
        }

        val result = mutableMapOf<String, List<Pair<Double, String>>>()
        for ((friendId, friendOps) in opsByFriend) {
            val confirmedEntries = confirmedFriendBase[friendId]
            val confirmedBase    = confirmedEntries?.sumOf { it.first } ?: 0.0
            val confirmedCur     = confirmedEntries?.maxByOrNull { kotlin.math.abs(it.first) }?.second

            val pendingCurrencies = friendOps.mapNotNull { op ->
                val r = op.localResourceId ?: op.serverResourceId ?: return@mapNotNull null
                expenseCache[r]?.currency
            }.toSet()

            val displayCurrency = confirmedCur
                ?: pendingCurrencies.singleOrNull()
                ?: continue  // mixed or unknown currency — skip

            var delta = 0.0
            for (op in friendOps) {
                val r       = op.localResourceId ?: op.serverResourceId ?: continue
                val expense = expenseCache[r] ?: continue
                if (expense.currency != displayCurrency) continue
                delta += opDelta(op, expense, impacts)
            }
            result[friendId] = listOf(Pair(confirmedBase + delta, displayCurrency))
        }
        return result
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun buildDeltasByCurrency(
        ops          : List<PendingOperationEntity>,
        expenseCache : Map<String, Expense>,
        impacts      : Map<String, PendingBalanceImpactEntity>,
    ): Map<String, Double> {
        val deltas = mutableMapOf<String, Double>()
        for (op in ops) {
            val resource = op.localResourceId ?: op.serverResourceId ?: continue
            val expense  = expenseCache[resource] ?: continue
            val d = opDelta(op, expense, impacts)
            if (d == 0.0) continue
            deltas[expense.currency] = (deltas[expense.currency] ?: 0.0) + d
        }
        return deltas
    }

    private fun opDelta(
        op      : PendingOperationEntity,
        expense : Expense,
        impacts : Map<String, PendingBalanceImpactEntity>,
    ): Double = when (op.operationType) {
        "CREATE_EXPENSE"  -> expense.yourBalance
        "DELETE_EXPENSE"  -> -expense.yourBalance
        "RESTORE_EXPENSE" -> expense.yourBalance
        "UPDATE_EXPENSE"  -> impacts[op.operationId]?.delta ?: 0.0
        else              -> 0.0
    }
}

/** Result of [EffectiveBalanceCalculator.globalEffectiveSummary]. */
data class GlobalEffectiveResult(
    val owedToMe : Double,
    val youOwe   : Double,
    val entries  : List<BalanceCurrencyEntry>,
)