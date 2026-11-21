package dev.qtremors.eonslate.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Seven pill bars, one per weekday in the week's order, with the day's letter under each and the
 * busiest one in the accent. [peakLabel] names the tallest, such as "₹4,200".
 */
@Composable
fun WeekdayBars(
    totals: List<Pair<DayOfWeek, Double>>,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    peakLabel: String? = null,
) {
    val busiest = totals.indices.maxByOrNull { totals[it].second }?.takeIf { totals[it].second > 0 }
    Column(modifier) {
        BarChart(
            values = totals.map { it.second.toFloat() },
            color = color.copy(alpha = 0.55f),
            contentDescription = contentDescription,
            highlight = busiest,
            highlightColor = color,
            height = 110.dp,
            peakLabel = peakLabel,
        )
        Row(Modifier.fillMaxWidth()) {
            totals.forEach { (day, _) ->
                Text(
                    day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
