package dev.qtremors.eonslate.core.tracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.qtremors.eonslate.core.tracker.domain.insights.GoalProgress
import dev.qtremors.eonslate.core.tracker.domain.model.FieldDefinition
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue
import dev.qtremors.eonslate.core.tracker.domain.model.GoalPeriod
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay

/** A goal amount in the field's own units: "5 glasses", "1 h 30 m". */
@Composable
fun goalAmount(field: FieldDefinition, amount: Long): String {
    val value = if (field.type == FieldType.DURATION) FieldValue.Duration(amount) else FieldValue.Number(amount)
    return ValueFormat.format(field, value, rememberValueWords(), LocalValueDisplay.current.numberLocale)
}

/** Where a goal stands: "5 glasses of 8 glasses today", "1 coffee, at most 2 coffees today". */
@Composable
fun goalText(progress: GoalProgress): String {
    val total = goalAmount(progress.field, progress.total)
    val target = goalAmount(progress.field, progress.goal.target)
    val week = progress.goal.per == GoalPeriod.WEEK
    return stringResource(
        when {
            progress.goal.atMost && week -> R.string.goal_limit_week
            progress.goal.atMost -> R.string.goal_limit_day
            week -> R.string.goal_progress_week
            else -> R.string.goal_progress_day
        },
        total, target,
    )
}
