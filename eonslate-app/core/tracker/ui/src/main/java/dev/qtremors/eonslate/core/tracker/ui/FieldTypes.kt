package dev.qtremors.eonslate.core.tracker.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType

/** How each field type is named and drawn across the app. */
object FieldTypes {
    @StringRes
    fun labelRes(type: FieldType): Int = when (type) {
        FieldType.NUMBER -> R.string.field_type_number
        FieldType.MONEY -> R.string.field_type_money
        FieldType.DURATION -> R.string.field_type_duration
        FieldType.RATING -> R.string.field_type_rating
        FieldType.YES_NO -> R.string.field_type_yes_no
        FieldType.SINGLE_CHOICE -> R.string.field_type_single_choice
        FieldType.MULTI_CHOICE -> R.string.field_type_multi_choice
        FieldType.TEXT -> R.string.field_type_text
        FieldType.DATE -> R.string.field_type_date
    }

    fun icon(type: FieldType): ImageVector = when (type) {
        FieldType.NUMBER -> Icons.Rounded.Tag
        FieldType.MONEY -> Icons.Rounded.Payments
        FieldType.DURATION -> Icons.Rounded.Timer
        FieldType.RATING -> Icons.Rounded.Star
        FieldType.YES_NO -> Icons.Rounded.CheckCircle
        FieldType.SINGLE_CHOICE -> Icons.Rounded.RadioButtonChecked
        FieldType.MULTI_CHOICE -> Icons.Rounded.Checklist
        FieldType.TEXT -> Icons.AutoMirrored.Rounded.Notes
        FieldType.DATE -> Icons.Rounded.CalendarToday
    }
}

@Composable
fun FieldType.label(): String = stringResource(FieldTypes.labelRes(this))
