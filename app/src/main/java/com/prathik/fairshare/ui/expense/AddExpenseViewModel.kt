package com.prathik.fairshare.ui.expense

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prathik.fairshare.data.local.EncryptedTokenStore
import com.prathik.fairshare.data.model.request.CreateExpenseRequest
import com.prathik.fairshare.data.sync.OperationType
import com.prathik.fairshare.data.sync.PendingOperationRepository
import com.prathik.fairshare.data.sync.SyncWorker
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.Friend
import com.prathik.fairshare.domain.model.Group
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.model.Receipt
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.repository.ExpenseRepository
import com.prathik.fairshare.domain.usecase.expense.CreateExpenseUseCase
import com.prathik.fairshare.domain.usecase.friend.GetFriendsUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupMembersUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupsUseCase
import com.prathik.fairshare.domain.usecase.group.GetGroupUseCase
import com.prathik.fairshare.domain.usecase.receipt.ScanReceiptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val createExpenseUseCase: CreateExpenseUseCase,
    private val expenseRepository: ExpenseRepository,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getGroupUseCase: GetGroupUseCase,
    private val getGroupMembersUseCase: GetGroupMembersUseCase,
    private val getGroupBalancesUseCase: com.prathik.fairshare.domain.usecase.group.GetGroupBalancesUseCase,
    private val getFriendsUseCase: GetFriendsUseCase,
    private val scanReceiptUseCase: ScanReceiptUseCase,
    private val tokenStore: EncryptedTokenStore,
    private val pendingOperationRepository: PendingOperationRepository,
    private val syncManager              : com.prathik.fairshare.data.sync.FairShareSyncManager,
    private val json: Json,
    @ApplicationContext private val appContext: Context,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val preselectedGroupId: String? = savedStateHandle.get<String>("groupId")
        ?.takeIf { it.isNotBlank() }

    val preselectedFriendId: String? = savedStateHandle.get<String>("friendId")
        ?.takeIf { it.isNotBlank() }

    val currentUserId: String? = tokenStore.getUserId()

    private val _activeTab = MutableStateFlow(ExpenseTab.EXPENSE)
    val activeTab: StateFlow<ExpenseTab> = _activeTab.asStateFlow()

    fun onTabChanged(tab: ExpenseTab) {
        _activeTab.value = tab
    }

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _currency = MutableStateFlow(tokenStore.getPreferredCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _selectedGroupId = MutableStateFlow(preselectedGroupId)
    val selectedGroupId: StateFlow<String?> = _selectedGroupId.asStateFlow()

    private val _splitType = MutableStateFlow(SplitType.EQUAL)
    val splitType: StateFlow<SplitType> = _splitType.asStateFlow()

    private val _category = MutableStateFlow<ExpenseCategory?>(null)
    val category: StateFlow<ExpenseCategory?> = _category.asStateFlow()

    private var categoryManuallySet = false

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    // null = not recurring; "DAILY"/"WEEKLY"/"MONTHLY" = recurring
    private val _repeatInterval = MutableStateFlow<String?>(null)
    val repeatInterval: StateFlow<String?> = _repeatInterval.asStateFlow()

    private val _expenseDate = MutableStateFlow(LocalDateTime.now().toString())
    val expenseDate: StateFlow<String> = _expenseDate.asStateFlow()

    private val _payerData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val payerData: StateFlow<Map<String, Double>> = _payerData.asStateFlow()

    private val _splitData = MutableStateFlow<Map<String, Double>>(emptyMap())
    val splitData: StateFlow<Map<String, Double>> = _splitData.asStateFlow()

    private val _equalExcluded = MutableStateFlow<Set<String>>(emptySet())
    val equalExcluded: StateFlow<Set<String>> = _equalExcluded.asStateFlow()

    fun onToggleEqualMember(userId: String) {
        val current = _equalExcluded.value.toMutableSet()
        if (current.contains(userId)) current.remove(userId) else current.add(userId)
        val includedCount = (_members.value.size - current.size)
        if (includedCount < 1) return
        _equalExcluded.value = current
        recalculateSplits()
    }

    private var _pointerAtCreation: Int? = null

    private val _transferFromId = MutableStateFlow(currentUserId)
    val transferFromId: StateFlow<String?> = _transferFromId.asStateFlow()

    private val _transferToId = MutableStateFlow<String?>(null)
    val transferToId: StateFlow<String?> = _transferToId.asStateFlow()

    fun onTransferFromChanged(userId: String) {
        _transferFromId.value = userId
    }

    fun onTransferToChanged(userId: String) {
        _transferToId.value = userId
    }

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _receiptState = MutableStateFlow<ReceiptScanState>(ReceiptScanState.Idle)
    val receiptState: StateFlow<ReceiptScanState> = _receiptState.asStateFlow()

    private var scannedReceiptId: String? = null
    val currentReceiptId: String? get() = scannedReceiptId

    /**
     * Path to the cached scan JPEG, backed by [SavedStateHandle] so it survives
     * both normal backgrounding and process death/ViewModel recreation.
     * Key: "receipt_scan_file_path".
     *
     * Cleared on scan success, permanent (non-retryable) failure, and new scan start.
     * NOT deleted on [onCleared] so that lifecycle recreation can restore a retryable
     * state. May linger in cacheDir if the user abandons Add Expense after a scan
     * failure — this is safe to overwrite on the next scan attempt.
     */
    private var cachedScanFile: File?
        get() = savedStateHandle.get<String>("receipt_scan_file_path")?.let(::File)
        set(value) { savedStateHandle["receipt_scan_file_path"] = value?.absolutePath }

    /**
     * Monotonically increasing counter. Each [scanReceipt] call increments it.
     * Only the coroutine that started with the current generation may update
     * [_receiptState], so stale results from cancelled/slow scans are silently dropped.
     */
    private val scanGeneration = AtomicLong(0L)
    private var scanJob: Job? = null

    init {
        // If the process was killed while a scan was in-flight and a cache file still
        // exists on disk, restore a retryable inline error so the user can tap retry
        // without re-opening the scanner.
        val file = cachedScanFile
        if (file != null && file.exists()) {
            _receiptState.value = ReceiptScanState.Error(
                message        = "Scan paused — tap to retry when back online.",
                canRetry       = true,
                isNetworkError = true,
            )
        }
    }

    // itemId → list of userIds assigned to that item (for backend item-linking)
    private val _itemAssignments = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val itemAssignments: StateFlow<Map<String, List<String>>> = _itemAssignments.asStateFlow()

    // Accurate per-person dollar amounts from item assignment screen.
    // Sent as splitData to bypass the backend's broken all-member auto-computation.
    private val _itemSplitData = MutableStateFlow<Map<String, Double>>(emptyMap())

    /**
     * Called from ItemAssignmentScreen onDone.
     * assignments → item-level user lists for backend item-linking
     * splitData   → accurate per-person dollar totals; sent as splitData to backend
     *               so only assigned members appear in the expense split.
     */
    fun setItemAssignments(
        assignments: Map<String, List<String>>,
        splitData: Map<String, Double>,
    ) {
        _itemAssignments.value = assignments
        _itemSplitData.value = splitData
    }

    private val _uiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Idle)
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    /**
     * True when a group is selected but its members could not be loaded because
     * the device is offline and the cache is empty (group was never opened online).
     * The screen uses this to show a contextual message and disable Save.
     */
    private val _membersOfflineUnavailable = MutableStateFlow(false)
    val membersOfflineUnavailable: StateFlow<Boolean> = _membersOfflineUnavailable.asStateFlow()

    private val _preselectedFriend = MutableStateFlow<Friend?>(null)
    val preselectedFriend: StateFlow<Friend?> = _preselectedFriend.asStateFlow()

    init {
        loadGroups()
        preselectedGroupId?.let {
            loadMembers(it)
            loadGroupCurrency(it)
        }
        preselectedFriendId?.let { loadFriendAsSplitParticipant(it) }
    }

    /**
     * Pre-fill currency from group.defaultCurrency — set by any member in Group Settings.
     * Falls back to balance currency for existing multi-currency groups,
     * then to device locale if no balances exist.
     */
    private fun loadGroupCurrency(groupId: String) {
        viewModelScope.launch {
            // 1. Try group.defaultCurrency first (explicit user preference)
            when (val groupResult = getGroupUseCase(groupId)) {
                is ApiResult.Success -> {
                    val defaultCurrency = groupResult.data.defaultCurrency
                    if (defaultCurrency.isNotBlank()) {
                        _currency.value = defaultCurrency
                        return@launch
                    }
                }
                else -> Unit
            }
            // 2. Fall back to existing balance currency
            when (val result = getGroupBalancesUseCase(groupId)) {
                is ApiResult.Success -> {
                    val groupCurrency = result.data.firstOrNull()?.currency
                    if (groupCurrency != null) {
                        _currency.value = groupCurrency
                    }
                }
                else -> Unit
            }
        }
    }

    private fun loadFriendAsSplitParticipant(friendId: String) {
        viewModelScope.launch {
            when (val result = getFriendsUseCase()) {
                is ApiResult.Success -> {
                    val friend = result.data.find { it.id == friendId } ?: return@launch
                    val currentId = currentUserId ?: return@launch
                    _preselectedFriend.value = friend

                    val now = java.time.LocalDateTime.now().toString()
                    val youAsMember = com.prathik.fairshare.domain.model.GroupMember(
                        id = currentId,
                        userId = currentId,
                        fullName = "You",
                        email = "",
                        profilePictureUrl = null,
                        joinedAt = now,
                    )
                    val friendAsMember = com.prathik.fairshare.domain.model.GroupMember(
                        id = friendId,
                        userId = friendId,
                        fullName = friend.fullName,
                        email = friend.email,
                        profilePictureUrl = friend.profilePictureUrl,
                        joinedAt = now,
                    )
                    _members.value = listOf(youAsMember, friendAsMember)
                    _payerData.value = mapOf(currentId to 0.0)
                    recalculateSplits()
                }

                else -> Unit
            }
        }
    }

    fun onDescriptionChanged(value: String) {
        _description.value = value
        if (!categoryManuallySet) {
            _category.value = detectCategory(value)
        }
    }

    fun onNotesChanged(value: String) {
        _notes.value = value
    }

    fun onRepeatIntervalChanged(value: String?) {
        _repeatInterval.value = value
    }

    fun onDateChanged(value: String) {
        _expenseDate.value = value
    }

    fun onCurrencyChanged(value: String) {
        _currency.value = value
    }

    fun onAmountChanged(value: String) {
        _amount.value = value
        recalculateSplits()
    }

    fun onCategoryChanged(value: ExpenseCategory?) {
        _category.value = value
        categoryManuallySet = value != null
    }

    fun onSplitTypeChanged(value: SplitType) {
        val previous = _splitType.value
        _splitType.value = value
        if (previous != value) {
            if (previous == SplitType.EQUAL && value != SplitType.EQUAL) {
                _splitData.value = emptyMap()
            } else if (previous != SplitType.EQUAL && value != SplitType.EQUAL) {
                _splitData.value = emptyMap()
            }
        }
        recalculateSplits()
    }

    fun onGroupSelected(groupId: String) {
        _selectedGroupId.value = groupId
        loadMembers(groupId)
        loadGroupCurrency(groupId)
    }

    fun onPayerChanged(userId: String, amount: Double) {
        _payerData.value = _payerData.value.toMutableMap().apply {
            if (amount > 0) put(userId, amount) else remove(userId)
        }
    }

    fun onSplitChanged(userId: String, amount: Double) {
        _splitData.value = _splitData.value.toMutableMap().apply {
            if (amount > 0) put(userId, amount) else remove(userId)
        }
    }

    fun onSplitDataConfirmed(newSplitData: Map<String, Double>) {
        _splitData.value = newSplitData
    }

    private fun detectCategory(description: String): ExpenseCategory? {
        val lower = description.lowercase()
        return when {
            lower.containsAny(
                "dinner", "lunch", "breakfast", "restaurant", "food",
                "cafe", "coffee", "eat", "pizza", "burger", "sushi"
            ) -> ExpenseCategory.DINING_OUT

            lower.containsAny(
                "grocery", "groceries", "walmart", "target",
                "supermarket", "vegetables", "fruit", "milk"
            ) -> ExpenseCategory.GROCERIES

            lower.containsAny("uber", "lyft", "taxi", "cab", "ola", "rapido") -> ExpenseCategory.TAXI

            lower.containsAny("bus", "train", "metro", "subway", "commute") -> ExpenseCategory.BUS_TRAIN

            lower.containsAny("rent", "apartment", "flat", "lease") -> ExpenseCategory.RENT
            lower.containsAny("electric", "electricity", "power") -> ExpenseCategory.ELECTRICITY
            lower.containsAny("water") -> ExpenseCategory.WATER
            lower.containsAny("gas", "fuel", "petrol", "shell", "pump") -> ExpenseCategory.GAS_FUEL
            lower.containsAny(
                "internet", "wifi", "phone", "broadband", "sim"
            ) -> ExpenseCategory.TV_PHONE_INTERNET

            lower.containsAny(
                "movie", "netflix", "hulu", "cinema", "theatre",
                "amazon prime", "disney"
            ) -> ExpenseCategory.MOVIES

            lower.containsAny("parking", "park") -> ExpenseCategory.PARKING
            lower.containsAny(
                "hotel", "airbnb", "hostel", "motel", "resort"
            ) -> ExpenseCategory.HOTEL

            lower.containsAny(
                "flight", "plane", "airline", "airport", "indigo", "air india"
            ) -> ExpenseCategory.PLANE

            lower.containsAny(
                "medical", "doctor", "hospital", "pharmacy", "medicine", "clinic"
            ) -> ExpenseCategory.MEDICAL

            lower.containsAny(
                "gym", "sport", "football", "cricket", "badminton", "fitness"
            ) -> ExpenseCategory.SPORTS

            lower.containsAny("gift", "birthday", "present") -> ExpenseCategory.GIFTS
            lower.containsAny(
                "beer", "wine", "alcohol", "bar", "pub", "whiskey", "vodka"
            ) -> ExpenseCategory.LIQUOR

            lower.containsAny("pet", "dog", "cat", "vet") -> ExpenseCategory.PETS
            lower.containsAny(
                "cloth", "shirt", "shoes", "fashion", "zara", "h&m"
            ) -> ExpenseCategory.CLOTHING

            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }

    private fun loadGroups() {
        viewModelScope.launch {
            when (val result = getGroupsUseCase()) {
                is ApiResult.Success -> _groups.value = result.data
                else -> Unit
            }
        }
    }

    private fun loadMembers(groupId: String) {
        viewModelScope.launch {
            // Clear stale state from any previously selected group immediately so
            // the screen never shows members or allows Save for the wrong group.
            _members.value                   = emptyList()
            _membersOfflineUnavailable.value = false
            _payerData.value                 = emptyMap()
            _splitData.value                 = emptyMap()
            _equalExcluded.value             = emptySet()

            when (val result = getGroupMembersUseCase(groupId)) {
                is ApiResult.Success -> {
                    _members.value                   = result.data
                    // Mark unavailable if the repository returned success but with an
                    // empty list (shouldn't happen in practice, but safe to handle).
                    _membersOfflineUnavailable.value = result.data.isEmpty()
                    if (result.data.isNotEmpty()) {
                        val total = _amount.value.toDoubleOrNull() ?: 0.0
                        val currentUserMember = result.data.find { it.userId == currentUserId }
                        if (currentUserMember != null) {
                            _payerData.value = mapOf(currentUserMember.userId to total)
                        }
                        recalculateSplits()
                    }
                }

                else -> {
                    // Network failed and Room cache was empty (GroupRepositoryImpl
                    // returns Success with cached rows when they exist).
                    _membersOfflineUnavailable.value = true
                }
            }
        }
    }

    private fun recalculateSplits() {
        val total = _amount.value.toDoubleOrNull() ?: return
        val members = _members.value
        if (members.isEmpty()) return

        val uid = currentUserId ?: return
        if (_payerData.value.size == 1 && _payerData.value.containsKey(uid)) {
            _payerData.value = mapOf(uid to total)
        }

        when (_splitType.value) {
            SplitType.EQUAL -> {
                val included = members.filter { !_equalExcluded.value.contains(it.userId) }
                if (included.isEmpty()) return

                val totalCents = Math.round(total * 100)
                val shareCents = totalCents / included.size
                val remainderCents = totalCents - (shareCents * included.size)

                val pointer = _groups.value
                    .find { it.id == _selectedGroupId.value }
                    ?.lastRemainderIndex ?: 0
                val startIndex = if (remainderCents > 0 && included.isNotEmpty())
                    pointer % included.size else 0
                _pointerAtCreation = pointer

                val rotated = included.drop(startIndex) + included.take(startIndex)

                _splitData.value = rotated.mapIndexed { index, member ->
                    val amount = if (index < remainderCents) (shareCents + 1) / 100.0
                    else shareCents / 100.0
                    member.userId to amount
                }.toMap()
            }

            SplitType.UNEQUAL, SplitType.PERCENTAGE, SplitType.SHARES -> { /* manual */ }
        }
    }

    /**
     * Start a new receipt scan from the GMS scanner result [uri].
     *
     * Steps:
     * 1. Set [ReceiptScanState.Preparing] immediately (main thread).
     * 2. Cancel any in-flight scan; increment [scanGeneration].
     * 3. On IO: read URI → sampled-decode bitmap → compress JPEG → write to cacheDir.
     * 4. Store the file path via [cachedScanFile] (backed by SavedStateHandle).
     * 5. Upload via [scanReceiptUseCase]; only the current generation may update state.
     */
    fun scanReceipt(uri: Uri) {
        // Set Preparing synchronously BEFORE launching the coroutine so the button
        // gives immediate feedback even before IO work starts.
        _receiptState.value = ReceiptScanState.Preparing
        scanJob?.cancel()
        val myGeneration = scanGeneration.incrementAndGet()
        deleteCachedScanFile()
        scanJob = viewModelScope.launch {
            val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                writeScanToCache(uri, myGeneration)
            }
            // If a newer scan started while we were writing, discard this file.
            // The generation guard on file IO prevents stale writes from corrupting
            // the cache file that the newer scan will use.
            if (scanGeneration.get() != myGeneration) {
                file?.delete()
                return@launch
            }
            if (file == null) {
                _receiptState.value = ReceiptScanState.Error(
                    message  = "Could not read the image. Tap to scan again.",
                    canRetry = false,
                )
                return@launch
            }
            cachedScanFile = file
            uploadScanFile(file, myGeneration)
        }
    }

    /**
     * Retry the last scan using the persisted cache file on disk.
     * Safe after process death: checks [File.exists] before retrying.
     * If the file is gone, resets to [ReceiptScanState.Idle] so the Screen re-launches
     * the GMS scanner.
     */
    fun retryReceiptScan() {
        val file = cachedScanFile
        if (file == null || !file.exists()) {
            cachedScanFile = null
            _receiptState.value = ReceiptScanState.Idle
            return
        }
        scanJob?.cancel()
        val myGeneration = scanGeneration.incrementAndGet()
        scanJob = viewModelScope.launch {
            uploadScanFile(file, myGeneration)
        }
    }

    /**
     * Read [uri] via [appContext.contentResolver], decode the bitmap with inSampleSize
     * sampling (max dimension 1200px) to avoid OOM on large camera scans, compress
     * to JPEG at 75 quality, and write to cacheDir/fairshare_receipt_scan/.
     * Must run on a background dispatcher.
     *
     * Two-pass approach:
     * 1. Decode bounds only (no pixel allocation) to get image dimensions.
     * 2. Decode with computed [inSampleSize] so the full-resolution bitmap is
     *    never loaded into memory.
     */
    private fun writeScanToCache(uri: Uri, generation: Long): File? {
        return try {
            val cr = appContext.contentResolver

            // Pass 1: decode only bounds — zero pixel allocation.
            val bounds = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            cr.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, bounds)
            }
            val (rawW, rawH) = bounds.outWidth to bounds.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            // Compute power-of-2 sample size so the decoded bitmap fits within 1200px.
            val maxDim = 1200
            var sampleSize = 1
            var halfW = rawW / 2
            var halfH = rawH / 2
            while (halfW >= maxDim || halfH >= maxDim) {
                sampleSize *= 2
                halfW /= 2
                halfH /= 2
            }

            // Pass 2: decode sampled bitmap — never loads full resolution into memory.
            val opts = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            var bitmap = cr.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            // Fine-scale to exactly maxDim on the long edge if still over.
            val longEdge = maxOf(bitmap.width, bitmap.height)
            if (longEdge > maxDim) {
                val ratio = maxDim.toFloat() / longEdge
                bitmap = android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width  * ratio).toInt(),
                    (bitmap.height * ratio).toInt(),
                    true,
                )
            }

            val dir = File(appContext.cacheDir, "fairshare_receipt_scan").also { it.mkdirs() }
            // Generation-specific filename prevents stale cancelled-scan IO from
            // overwriting the file currently in use by the active scan.
            val outFile = File(dir, "receipt_scan_$generation.jpg")
            FileOutputStream(outFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
            }
            outFile
        } catch (_: Exception) { null }
    }

    /**
     * Read [file], Base64-encode it on [kotlinx.coroutines.Dispatchers.IO], and upload
     * to the Gemini receipt scan API.
     * Only updates [_receiptState] when [myGeneration] still matches [scanGeneration].
     */
    private suspend fun uploadScanFile(file: File, myGeneration: Long) {
        _receiptState.value = ReceiptScanState.Scanning
        val base64 = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try { android.util.Base64.encodeToString(file.readBytes(), android.util.Base64.NO_WRAP) }
            catch (_: Exception) { null }
        }
        if (scanGeneration.get() != myGeneration) return
        if (base64 == null) {
            deleteCachedScanFile()
            _receiptState.value = ReceiptScanState.Error(
                message  = "Could not read cached image. Tap to scan again.",
                canRetry = false,
            )
            return
        }
        try {
            when (val result = scanReceiptUseCase(
                imageBase64       = base64,
                mimeType          = "image/jpeg",
                preferredCurrency = _currency.value,
            )) {
                is ApiResult.Success -> {
                    if (scanGeneration.get() != myGeneration) return
                    val receipt = result.data
                    scannedReceiptId = receipt.id
                    receipt.merchantName?.let { _description.value = it }
                    if (receipt.totalAmount > 0) _amount.value = receipt.totalAmount.toString()
                    _expenseDate.value = java.time.LocalDateTime.now().toString()
                    receipt.currency?.let { _currency.value = it }
                    deleteCachedScanFile() // success — file no longer needed
                    _receiptState.value = ReceiptScanState.Success(receipt)
                    recalculateSplits()
                }
                is ApiResult.NetworkError -> {
                    if (scanGeneration.get() != myGeneration) return
                    // Keep cache file so retryReceiptScan() can re-upload without scanner relaunch.
                    _receiptState.value = ReceiptScanState.Error(
                        message        = "Scan paused — tap to retry when back online.",
                        canRetry       = true,
                        isNetworkError = true,
                    )
                }
                else -> {
                    if (scanGeneration.get() != myGeneration) return
                    deleteCachedScanFile() // permanent failure — payload won't help on retry
                    _receiptState.value = ReceiptScanState.Error(
                        message  = "Scan failed. Tap to scan again or enter details manually.",
                        canRetry = false,
                    )
                }
            }
        } catch (e: Exception) {
            if (scanGeneration.get() != myGeneration) return
            deleteCachedScanFile() // unexpected exception — payload won't help on retry
            _receiptState.value = ReceiptScanState.Error(
                message  = "Scan failed. Tap to scan again or enter details manually.",
                canRetry = false,
            )
        }
    }

    /** Delete the cache file and clear the reference. */
    private fun deleteCachedScanFile() {
        cachedScanFile?.delete()
        cachedScanFile = null
    }

    override fun onCleared() {
        super.onCleared()
        // Do NOT delete cachedScanFile here — the file path is persisted in
        // SavedStateHandle and the file itself must survive ViewModel recreation
        // (e.g. configuration change, process death) so retryReceiptScan() can
        // re-upload without forcing the user to re-open the scanner.
        // The file is deleted on success, permanent failure, or new scan start.
    }

    fun setScanError(message: String) {
        _receiptState.value = ReceiptScanState.Error(message, canRetry = false)
    }

    fun submit() {
        if (_uiState.value is AddExpenseUiState.Loading) return
        if (_activeTab.value == ExpenseTab.TRANSFER) submitTransfer() else submitExpense()
    }

    private fun submitExpense() {
        val groupId = _selectedGroupId.value
        val description = _description.value.trim()
        val amount = _amount.value.toDoubleOrNull()

        if (description.isBlank()) {
            _uiState.value = AddExpenseUiState.Error("Please enter a description."); return
        }
        if (amount == null || amount <= 0) {
            _uiState.value = AddExpenseUiState.Error("Please enter a valid amount."); return
        }
        if (_payerData.value.isEmpty()) {
            _uiState.value = AddExpenseUiState.Error("Please select who paid."); return
        }

        // Fix: if single payer was selected before amount was entered, their stored amount
        // is 0.0 or 1.0 (fallback). Correct it to the actual total now.
        if (_payerData.value.size == 1) {
            val (payerId, payerAmt) = _payerData.value.entries.first()
            if (payerAmt <= 1.0 || payerAmt != amount) {
                _payerData.value = mapOf(payerId to amount)
            }
        }

        // Determine the split data to send.
        val effectiveSplitData: Map<String, Double>? = when {
            _itemSplitData.value.isNotEmpty() -> _itemSplitData.value
            _splitData.value.isNotEmpty() -> _splitData.value
            else -> null
        }

        if (effectiveSplitData == null && _itemAssignments.value.isEmpty()) {
            _uiState.value = AddExpenseUiState.Error("Please set how to split."); return
        }

        val finalCategory = _category.value ?: ExpenseCategory.GENERAL
        val userId = currentUserId ?: run {
            _uiState.value = AddExpenseUiState.Error("Not logged in. Please sign in again.")
            return
        }

        _uiState.value = AddExpenseUiState.Loading  // set synchronously — prevents double-tap
        viewModelScope.launch {

            // Build request for JSON storage (needed for SyncWorker replay).
            val effectiveSplitType = if (_itemSplitData.value.isNotEmpty()) SplitType.UNEQUAL
            else _splitType.value
            val request = CreateExpenseRequest(
                groupId          = groupId,
                description      = description,
                totalAmount      = amount,
                currency         = _currency.value,
                splitType        = effectiveSplitType,
                category         = finalCategory,
                notes            = _notes.value.ifBlank { null },
                expenseDate      = _expenseDate.value,
                payerData        = _payerData.value,
                splitData        = effectiveSplitData,
                receiptId        = scannedReceiptId,
                remainderPointer = _pointerAtCreation,
                itemAssignments  = _itemAssignments.value.ifEmpty { null },
                repeatInterval   = _repeatInterval.value,
                // idempotencyKey is omitted here — it comes from the pending operation below
            )

            // Try-first: generate a stable idempotency key and local expense ID BEFORE
            // the network call, but do NOT enqueue yet. We only enqueue on NetworkError.
            // This means online success and HTTP errors never create a pending operation
            // or show a sync banner.
            //   localExpenseId — placeholder ExpenseEntity.id in Room (offline only)
            //   idempotencyKey — sent to backend; reused in queue row on NetworkError
            //                    so SyncWorker replay is deduplicated by the backend.
            val localExpenseId = java.util.UUID.randomUUID().toString()
            val idempotencyKey = java.util.UUID.randomUUID().toString()

            when (val result = createExpenseUseCase(
                groupId          = groupId,
                description      = description,
                totalAmount      = amount,
                currency         = _currency.value,
                splitType        = effectiveSplitType,
                category         = finalCategory,
                notes            = _notes.value.ifBlank { null },
                expenseDate      = _expenseDate.value,
                payerData        = _payerData.value,
                splitData        = effectiveSplitData,
                receiptId        = scannedReceiptId,
                idempotencyKey   = idempotencyKey,
                remainderPointer = _pointerAtCreation,
                itemAssignments  = _itemAssignments.value.ifEmpty { null },
                repeatInterval   = _repeatInterval.value,
            )) {
                is ApiResult.Success -> {
                    // Online success — no pending operation was created, nothing to mark.
                    // For direct friend expenses, propagate otherUserId from the local
                    // placeholder to the server-confirmed cached row so FriendDetail
                    // can find it by otherUserId immediately from Room.
                    if (groupId == null && preselectedFriendId != null) {
                        expenseRepository.setCachedDirectOtherUserId(
                            expenseId   = result.data.id,
                            otherUserId = preselectedFriendId,
                        )
                    }
                    // Navigate immediately — cache refresh runs in background.
                    _uiState.value = AddExpenseUiState.Success
                    // SyncManager owns backgroundScope — survives ViewModel teardown.
                    syncManager.launchSyncAfterExpenseCreate(
                        expense       = result.data,
                        groupId       = groupId,
                        currentUserId = userId,
                        payerIds      = _payerData.value.keys,
                        splitIds      = effectiveSplitData?.keys ?: emptySet(),
                    )
                }

                is ApiResult.NetworkError -> {
                    // True offline failure: NOW enqueue with the same idempotencyKey that
                    // was already sent to the backend, so SyncWorker replay is deduplicated.
                    val enqueued = pendingOperationRepository.enqueue(
                        userId           = userId,
                        operationType    = OperationType.CREATE_EXPENSE,
                        endpoint         = "/api/expenses",
                        method           = "POST",
                        requestBodyJson  = json.encodeToString(request),
                        localResourceId  = localExpenseId,
                        idempotencyKey   = idempotencyKey,
                    )
                    pendingOperationRepository.markRetryable(
                        operationId = enqueued.operationId,
                        error       = result.exception.message ?: "Network error",
                    )
                    // Insert a local placeholder so the expense appears in the list
                    // immediately with the pending-sync dot. Deleted by SyncWorker on success.
                    val yourPaid  = _payerData.value[userId] ?: 0.0
                    val yourShare = effectiveSplitData?.get(userId) ?: 0.0
                    // Build userId→name map so payer/split entities show real names offline.
                    val memberNames = _members.value.associate { it.userId to it.fullName }
                        .toMutableMap().also {
                            // Ensure current user maps to their full name.
                            it[userId] = tokenStore.getFullName() ?: "You"
                        }
                    expenseRepository.insertLocalPendingExpense(
                        localId     = localExpenseId,
                        groupId     = groupId,
                        description = description,
                        totalAmount = amount,
                        currency    = _currency.value,
                        splitType   = effectiveSplitType,
                        category    = finalCategory,
                        addedById   = userId,
                        addedByName = tokenStore.getFullName() ?: "You",
                        expenseDate = _expenseDate.value,
                        yourPaid    = yourPaid,
                        yourShare   = yourShare,
                        otherUserId = preselectedFriendId,
                        payerData   = _payerData.value,
                        splitData   = effectiveSplitData ?: emptyMap(),
                        memberNames = memberNames,
                    )
                    SyncWorker.triggerImmediateSync(appContext)
                    _uiState.value = AddExpenseUiState.SavedOffline
                }

                is ApiResult.ValidationError -> {
                    // Foreground HTTP error — no pending operation was created.
                    _uiState.value = AddExpenseUiState.Error(result.message)
                }

                is ApiResult.Forbidden -> {
                    _uiState.value = AddExpenseUiState.Error(result.message)
                }

                is ApiResult.Unauthorized -> {
                    _uiState.value = AddExpenseUiState.Error(result.message)
                }

                is ApiResult.Conflict -> {
                    // Preserve the backend actual 409 reason.
                    _uiState.value = AddExpenseUiState.Error(result.message)
                }

                else -> {
                    _uiState.value = AddExpenseUiState.Error("Failed to create expense. Please try again.")
                }
            }
        }
    }

    private fun submitTransfer() {
        val groupId = _selectedGroupId.value
        val fromId = _transferFromId.value
        val toId = _transferToId.value
        val amount = _amount.value.toDoubleOrNull()

        if (fromId == null) {
            _uiState.value = AddExpenseUiState.Error("Please select who is paying."); return
        }
        if (toId == null) {
            _uiState.value = AddExpenseUiState.Error("Please select who receives."); return
        }
        if (fromId == toId) {
            _uiState.value =
                AddExpenseUiState.Error("From and To cannot be the same person."); return
        }
        if (amount == null || amount <= 0) {
            _uiState.value = AddExpenseUiState.Error("Please enter a valid amount."); return
        }

        val userId = currentUserId ?: run {
            _uiState.value = AddExpenseUiState.Error("Not logged in. Please sign in again.")
            return
        }

        val transferDescription =
            "${members.value.find { it.userId == fromId }?.fullName ?: "Someone"} → " +
                    "${members.value.find { it.userId == toId }?.fullName ?: "Someone"}"

        _uiState.value = AddExpenseUiState.Loading  // set synchronously — prevents double-tap
        viewModelScope.launch {

            val request = CreateExpenseRequest(
                groupId     = groupId,
                description = transferDescription,
                totalAmount = amount,
                currency    = _currency.value,
                splitType   = SplitType.UNEQUAL,
                category    = ExpenseCategory.GENERAL,
                notes       = _notes.value.ifBlank { null },
                expenseDate = _expenseDate.value,
                payerData   = mapOf(fromId to amount),
                splitData   = mapOf(toId to amount),
            )

            // Try-first: generate stable keys upfront, enqueue only on NetworkError.
            val localExpenseId = java.util.UUID.randomUUID().toString()
            val idempotencyKey = java.util.UUID.randomUUID().toString()

            when (val result = createExpenseUseCase(
                groupId        = groupId,
                description    = transferDescription,
                totalAmount    = amount,
                currency       = _currency.value,
                splitType      = SplitType.UNEQUAL,
                category       = ExpenseCategory.GENERAL,
                notes          = _notes.value.ifBlank { null },
                expenseDate    = _expenseDate.value,
                payerData      = mapOf(fromId to amount),
                splitData      = mapOf(toId to amount),
                receiptId      = null,
                idempotencyKey = idempotencyKey,
            )) {
                is ApiResult.Success -> {
                    // Online success — no pending operation was created.
                    if (groupId == null && preselectedFriendId != null) {
                        expenseRepository.setCachedDirectOtherUserId(
                            expenseId   = result.data.id,
                            otherUserId = preselectedFriendId,
                        )
                    }
                    _uiState.value = AddExpenseUiState.Success
                    syncManager.launchSyncAfterExpenseCreate(
                        expense       = result.data,
                        groupId       = groupId,
                        currentUserId = userId,
                        payerIds      = setOf(fromId),
                        splitIds      = setOf(toId),
                    )
                }

                is ApiResult.NetworkError -> {
                    val enqueued = pendingOperationRepository.enqueue(
                        userId           = userId,
                        operationType    = OperationType.CREATE_EXPENSE,
                        endpoint         = "/api/expenses",
                        method           = "POST",
                        requestBodyJson  = json.encodeToString(request),
                        localResourceId  = localExpenseId,
                        idempotencyKey   = idempotencyKey,
                    )
                    pendingOperationRepository.markRetryable(
                        enqueued.operationId, result.exception.message ?: "Network error"
                    )
                    // Insert placeholder so the transfer appears in list immediately.
                    val yourPaid  = if (fromId == userId) amount else 0.0
                    val yourShare = if (toId  == userId) amount else 0.0
                    val memberNamesDirect = _members.value.associate { it.userId to it.fullName }
                        .toMutableMap().also { it[userId] = tokenStore.getFullName() ?: "You" }
                    expenseRepository.insertLocalPendingExpense(
                        localId     = localExpenseId,
                        groupId     = groupId,
                        description = transferDescription,
                        totalAmount = amount,
                        currency    = _currency.value,
                        splitType   = com.prathik.fairshare.domain.model.SplitType.UNEQUAL,
                        category    = com.prathik.fairshare.domain.model.ExpenseCategory.GENERAL,
                        addedById   = userId,
                        addedByName = tokenStore.getFullName() ?: "You",
                        expenseDate = _expenseDate.value,
                        yourPaid    = yourPaid,
                        yourShare   = yourShare,
                        otherUserId = preselectedFriendId,
                        payerData   = mapOf(fromId to amount),
                        splitData   = mapOf(toId to amount),
                        memberNames = memberNamesDirect,
                    )
                    SyncWorker.triggerImmediateSync(appContext)
                    _uiState.value = AddExpenseUiState.SavedOffline
                }

                else -> {
                    _uiState.value = AddExpenseUiState.Error("Failed to save transfer. Please try again.")
                }
            }
        }
    }

    fun resetUiState() {
        _uiState.value = AddExpenseUiState.Idle
    }
}

enum class ExpenseTab { EXPENSE, TRANSFER }

sealed class AddExpenseUiState {
    object Idle : AddExpenseUiState()
    object Loading : AddExpenseUiState()
    object Success : AddExpenseUiState()
    /** Wave 2D-3: expense saved locally and queued for sync when network returns. */
    object SavedOffline : AddExpenseUiState()
    data class Error(val message: String) : AddExpenseUiState()
}

sealed class ReceiptScanState {
    /** No scan in progress and no result yet. */
    object Idle : ReceiptScanState()
    /** Image bytes are being read / compressed off the main thread. */
    object Preparing : ReceiptScanState()
    /** Compressed payload is uploading to the Gemini API. */
    object Scanning : ReceiptScanState()
    data class Success(val receipt: Receipt) : ReceiptScanState()
    /**
     * @param message        Inline banner text — never routed to the global error dialog.
     * @param canRetry       True when the ViewModel has a cached payload for [AddExpenseViewModel.retryReceiptScan].
     * @param isNetworkError True when the failure was a transient network issue (retry makes sense).
     */
    data class Error(
        val message      : String,
        val canRetry     : Boolean = false,
        val isNetworkError: Boolean = false,
    ) : ReceiptScanState()
}