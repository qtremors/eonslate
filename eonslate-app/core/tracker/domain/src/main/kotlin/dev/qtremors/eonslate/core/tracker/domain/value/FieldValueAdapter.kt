package dev.qtremors.eonslate.core.tracker.domain.value

import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldValue

/**
 * Carries a logged value across a change to its field's config, so editing a tracker never leaves
 * stored numbers meaning something else.
 *
 * Only changes that alter what a stored number means are applied: a new number of decimals or a
 * currency with different minor digits rescales the value, a removed choice option drops the
 * selection, and a new type drops the value entirely. Limits such as min, max, rating scale and
 * text length apply to new input only, so tightening them never deletes what was logged.
 */
object FieldValueAdapter {

    /** True when values logged under [old] must be rewritten or dropped under [new]. */
    fun needsAdapting(old: FieldConfig, new: FieldConfig): Boolean = when {
        old.type != new.type -> true
        old is FieldConfig.Number && new is FieldConfig.Number -> old.decimals != new.decimals
        old is FieldConfig.Money && new is FieldConfig.Money ->
            MoneyAmount.fractionDigits(old.currencyCode) != MoneyAmount.fractionDigits(new.currencyCode)
        old is FieldConfig.SingleChoice && new is FieldConfig.SingleChoice ->
            !new.options.map { it.id }.containsAll(old.options.map { it.id })
        old is FieldConfig.MultiChoice && new is FieldConfig.MultiChoice ->
            !new.options.map { it.id }.containsAll(old.options.map { it.id })
        else -> false
    }

    /**
     * The value to keep for [value], logged under [old], now that the field uses [new]; or null
     * when it can no longer be kept. Rescaling that loses digits rounds half up.
     */
    fun adapt(old: FieldConfig, new: FieldConfig, value: FieldValue): FieldValue? {
        if (old.type != new.type || value.type != new.type) return null
        return when (value) {
            is FieldValue.Number -> {
                val from = (old as FieldConfig.Number).decimals
                val to = (new as FieldConfig.Number).decimals
                ScaledDecimal.rescale(value.scaled, from, to)?.let(FieldValue::Number)
            }
            is FieldValue.Money -> {
                val from = MoneyAmount.fractionDigits((old as FieldConfig.Money).currencyCode)
                val to = MoneyAmount.fractionDigits((new as FieldConfig.Money).currencyCode)
                ScaledDecimal.rescale(value.minor, from, to)?.let(FieldValue::Money)
            }
            is FieldValue.SingleChoice -> {
                val known = (new as FieldConfig.SingleChoice).options.any { it.id == value.optionId }
                if (known) value else null
            }
            is FieldValue.MultiChoice -> {
                val known = (new as FieldConfig.MultiChoice).options.mapTo(mutableSetOf()) { it.id }
                val kept = value.optionIds.filterTo(mutableSetOf(), known::contains)
                // An empty selection is still an answer ("none of these"), so it is kept.
                FieldValue.MultiChoice(kept)
            }
            is FieldValue.Duration,
            is FieldValue.Rating,
            is FieldValue.YesNo,
            is FieldValue.Text,
            is FieldValue.Date -> value
        }
    }
}
