package com.prathik.fairshare.ui.account

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prathik.fairshare.ui.components.FsAvatar
import com.prathik.fairshare.ui.components.FsLoadingScreen
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.ComponentSize
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface3
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary
import com.prathik.fairshare.util.MoneyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    onNavigateToEditProfile  : () -> Unit,
    onNavigateToQrCode       : () -> Unit,
    onNavigateToCurrency     : () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToMyAnalytics  : () -> Unit,
    onNavigateToImport       : () -> Unit,
    onLoggedOut              : () -> Unit,
    viewModel                : AccountViewModel = hiltViewModel(),
) {
    val isLoading      by viewModel.isLoading.collectAsState()
    val profile        by viewModel.profile.collectAsState()
    val balanceSummary by viewModel.balanceSummary.collectAsState()
    val actionState    by viewModel.actionState.collectAsState()
    val snackbarHost   = remember { SnackbarHostState() }
    val context        = LocalContext.current

    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadData() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is AccountActionState.Error   -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            is AccountActionState.Success -> { snackbarHost.showSnackbar(s.message); viewModel.resetActionState() }
            else -> Unit
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = Surface2,
            title = { Text("Log out?", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
            text  = { Text("You'll need to sign in again to access your account.", color = TextSecondary, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; viewModel.logout { onLoggedOut() } }) {
                    Text("Log out", color = Negative, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        )
    }

    Scaffold(
        containerColor = Surface0,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        topBar         = {
            FsTopBar(title = "Account")
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh    = { viewModel.loadData() },
            modifier     = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (isLoading && profile == null) {
                FsLoadingScreen()
                return@PullToRefreshBox
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {

                // ── Profile card ──────────────────────────────────────────────
                item {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(Surface2)
                            .clickable { onNavigateToEditProfile() }
                            .padding(horizontal = Spacing.md, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FsAvatar(
                            name     = profile?.fullName ?: "",
                            userId   = profile?.id ?: "",
                            size     = ComponentSize.avatarLg,
                        )
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = profile?.fullName ?: "",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary,
                            )
                            Text(
                                text     = profile?.email ?: "",
                                fontSize = 12.sp,
                                color    = TextTertiary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Text(text = "›", fontSize = 20.sp, color = TextTertiary)
                    }
                }

                // ── Overview ──────────────────────────────────────────────────
                item {
                    SectionLabel("OVERVIEW")
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .padding(bottom = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        // Total paid
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .padding(Spacing.md),
                        ) {
                            Text(text = "Total paid", fontSize = 11.sp, color = TextTertiary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text       = MoneyUtils.format(balanceSummary?.owedToMe ?: 0.0, balanceSummary?.currency ?: "USD"),
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Green400,
                            )
                            Text(
                                text     = "${(balanceSummary?.owedToMe?.let { (it / 50).toInt() } ?: 0)} expenses",
                                fontSize = 11.sp,
                                color    = TextTertiary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        // Total owed
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Radius.xl))
                                .background(Surface2)
                                .padding(Spacing.md),
                        ) {
                            Text(text = "Total owed", fontSize = 11.sp, color = TextTertiary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text       = MoneyUtils.format(balanceSummary?.youOwe ?: 0.0, balanceSummary?.currency ?: "USD"),
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Negative,
                            )
                            Text(
                                text     = "across all groups",
                                fontSize = 11.sp,
                                color    = TextTertiary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }

                // ── Settings ──────────────────────────────────────────────────
                item {
                    SectionLabel("SETTINGS")
                    AccountCard {
                        NavRow(
                            icon       = Icons.Outlined.QrCode,
                            iconBg     = Color(0xFF1A1A3A),
                            iconTint   = Color(0xFF7F77DD),
                            label      = "My QR code",
                            subtitle   = "Share your code to add friends",
                            onClick    = onNavigateToQrCode,
                        )
                        RowDivider()
                        NavRow(
                            icon       = Icons.Outlined.AttachMoney,
                            iconBg     = Color(0xFF2A1800),
                            iconTint   = Color(0xFFF0A500),
                            label      = "Default currency",
                            trailing   = profile?.preferredCurrency ?: "USD",
                            onClick    = onNavigateToCurrency,
                        )
                        RowDivider()
                        ToggleRow(
                            icon     = Icons.Outlined.Notifications,
                            iconBg   = Color(0xFF1A1A3A),
                            iconTint = Color(0xFF7F77DD),
                            label    = "Notifications",
                            checked  = profile?.notificationEnabled ?: true,
                            onToggle = { viewModel.toggleNotifications() },
                        )
                    }
                }

                // ── Features ──────────────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SectionLabel("FEATURES")
                    AccountCard {
                        NavRow(
                            icon     = Icons.Outlined.Analytics,
                            iconBg   = Color(0xFF1A3A1A),
                            iconTint = Green400,
                            label    = "My analytics",
                            onClick  = onNavigateToMyAnalytics,
                        )
                        RowDivider()
                        NavRow(
                            icon     = Icons.Outlined.Upload,
                            iconBg   = Color(0xFF2A1800),
                            iconTint = Color(0xFFF0A500),
                            label    = "Import from Splitwise",
                            onClick  = onNavigateToImport,
                        )
                    }
                }

                // ── About ─────────────────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    SectionLabel("ABOUT")
                    AccountCard {
                        Row(
                            modifier          = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBox(
                                icon   = Icons.Outlined.Info,
                                bg     = Surface4,
                                tint   = TextSecondary,
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(text = "App version", fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(text = "1.0.0", fontSize = 13.sp, color = TextTertiary)
                        }
                        RowDivider()
                        NavRow(
                            icon    = Icons.Outlined.Description,
                            iconBg  = Surface4,
                            iconTint = TextSecondary,
                            label   = "Privacy policy",
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://fairshare.app/privacy"))
                                context.startActivity(intent)
                            },
                        )
                    }
                }

                // ── Log out ───────────────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.lg)
                            .clip(RoundedCornerShape(Radius.xl))
                            .background(Surface2)
                            .clickable { showLogoutDialog = true }
                            .padding(vertical = Spacing.md),
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Logout,
                                contentDescription = null,
                                tint               = Negative,
                                modifier           = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text       = "Log out",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Negative,
                            )
                        }
                    }
                }

                // ── Deactivate ────────────────────────────────────────────────
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.md),
                    ) {
                        Text(
                            text     = "Deactivate account",
                            fontSize = 13.sp,
                            color    = TextTertiary.copy(alpha = 0.5f),
                        )
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        fontWeight    = FontWeight.SemiBold,
        color         = TextTertiary,
        letterSpacing = 1.sp,
        modifier      = Modifier.padding(
            start  = Spacing.lg + 2.dp,
            bottom = 6.dp,
        ),
    )
}

@Composable
private fun AccountCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg)
            .clip(RoundedCornerShape(Radius.xl))
            .background(Surface2)
            .padding(horizontal = Spacing.md),
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color     = Surface3,
        thickness = 0.5.dp,
        modifier  = Modifier.padding(start = 46.dp),
    )
}

@Composable
private fun IconBox(
    icon: ImageVector,
    bg  : Color,
    tint: Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier         = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(Radius.sm))
            .background(bg),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = tint,
            modifier           = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun NavRow(
    icon    : ImageVector,
    iconBg  : Color,
    iconTint: Color,
    label   : String,
    subtitle: String? = null,
    trailing: String? = null,
    onClick : () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon = icon, bg = iconBg, tint = iconTint)
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 14.sp, color = TextPrimary)
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = TextTertiary)
            }
        }
        if (trailing != null) {
            Text(text = trailing, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(end = 4.dp))
        }
        Text(text = "›", fontSize = 18.sp, color = TextTertiary)
    }
}

@Composable
private fun ToggleRow(
    icon    : ImageVector,
    iconBg  : Color,
    iconTint: Color,
    label   : String,
    checked : Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBox(icon = icon, bg = iconBg, tint = iconTint)
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(text = label, fontSize = 14.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = Green400,
                uncheckedThumbColor  = TextTertiary,
                uncheckedTrackColor  = Surface4,
                uncheckedBorderColor = Surface4,
            ),
        )
    }
}