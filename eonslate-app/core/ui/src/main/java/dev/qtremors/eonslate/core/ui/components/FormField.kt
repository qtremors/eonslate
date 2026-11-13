package dev.qtremors.eonslate.core.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import dev.qtremors.eonslate.core.ui.theme.FieldShape

/**
 * The app's text field: outlined, always the 16 dp [FieldShape], with its problem shown
 * underneath in place of the hint. Pass [error] to show a problem; null shows [supporting].
 */
@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    error: String? = null,
    placeholder: String? = null,
    suffix: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLength: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { next -> if (maxLength == null || next.length <= maxLength) onValueChange(next) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        suffix = suffix?.let { { Text(it) } },
        supportingText = (error ?: supporting)?.let { { Text(it) } },
        isError = error != null,
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        trailingIcon = trailingIcon,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        shape = FieldShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            errorContainerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        modifier = modifier.fillMaxWidth()
    )
}
