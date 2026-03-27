package com.prathik.fairshare.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Negative
import com.prathik.fairshare.ui.theme.Radius
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary
import com.prathik.fairshare.ui.theme.TextTertiary

/**
 * Standard text field used throughout the app.
 *
 * Shows a red error message below the field when [error] is not null.
 * All keyboard options are configurable — email, phone, number, etc.
 */
@Composable
fun FsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    error: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value                = value,
        onValueChange        = onValueChange,
        modifier             = modifier.fillMaxWidth(),
        label                = { Text(label) },
        placeholder          = { Text(placeholder, color = TextTertiary) },
        leadingIcon          = leadingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null, tint = TextSecondary) }
        },
        trailingIcon         = trailingIcon,
        isError              = error != null,
        supportingText       = error?.let { { Text(it, color = Negative) } },
        enabled              = enabled,
        singleLine           = singleLine,
        maxLines             = maxLines,
        shape                = RoundedCornerShape(Radius.lg),
        visualTransformation = visualTransformation,
        keyboardOptions      = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction    = imeAction,
        ),
        keyboardActions      = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = Green400,
            unfocusedBorderColor    = Surface4,
            errorBorderColor        = Negative,
            focusedLabelColor       = Green400,
            unfocusedLabelColor     = TextSecondary,
            errorLabelColor         = Negative,
            focusedTextColor        = TextPrimary,
            unfocusedTextColor      = TextPrimary,
            cursorColor             = Green400,
            focusedContainerColor   = Surface2,
            unfocusedContainerColor = Surface2,
            errorContainerColor     = Surface2,
        ),
    )
}

/**
 * Password field with visibility toggle.
 * Extends FsTextField — same styling, adds show/hide icon.
 */
@Composable
fun FsPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var isVisible by remember { mutableStateOf(false) }

    FsTextField(
        value                = value,
        onValueChange        = onValueChange,
        label                = label,
        modifier             = modifier,
        error                = error,
        enabled              = enabled,
        keyboardType         = KeyboardType.Password,
        imeAction            = imeAction,
        keyboardActions      = keyboardActions,
        visualTransformation = if (isVisible) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    imageVector        = if (isVisible) Icons.Default.Visibility
                    else Icons.Default.VisibilityOff,
                    contentDescription = if (isVisible) "Hide password" else "Show password",
                    tint               = TextSecondary,
                )
            }
        },
    )
}
/**
 * Amount input field — numeric decimal keyboard, max 2 decimal places.
 * Rejects input that would result in more than 2 decimal places.
 * Used in AddExpenseScreen, PartialSettleScreen, split amount inputs.
 */
@Composable
fun FsAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    FsTextField(
        value         = value,
        onValueChange = { input ->
            // Allow empty, digits, and at most one decimal point with max 2 decimal places
            val regex = Regex("^\\d*(\\.\\d{0,2})?$")
            if (input.isEmpty() || regex.matches(input)) {
                onValueChange(input)
            }
        },
        label        = label,
        modifier     = modifier,
        error        = error,
        enabled      = enabled,
        keyboardType = KeyboardType.Decimal,
        imeAction    = imeAction,
        keyboardActions = keyboardActions,
    )
}