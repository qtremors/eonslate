package dev.qtremors.eonslate.core.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.insights.PartStatus
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import java.time.temporal.ChronoUnit

/**
 * Where a part stands, in a few words: what is left before its interval ("1,240 km left",
 * "In 121 days"), else when its rhythm says it comes next, else when it was last logged.
 */
@Composable
fun partText(part: PartStatus, tracker: Tracker): String {
    val interval = part.interval
    val rhythm = part.rhythm
    val last = part.last
    return when {
        interval != null && interval.last != null -> {
            val days = interval.daysLeft
            // Far-off dates read the way people say them: "In 22 months" rather than in days.
            if (days != null && days > MONTHS_FROM && !interval.leadsByAmount) stringResource(R.string.part_in, gapText(days))
            else intervalRemaining(interval, tracker)
        }
        rhythm != null -> stringResource(R.string.rhythm_next_on, shortDate(rhythm.nextOn)).replaceFirstChar { it.uppercase() }
        last != null -> {
            val ago = ChronoUnit.DAYS.between(last, java.time.LocalDate.now())
            if (ago > MONTHS_FROM) stringResource(R.string.part_ago, gapText(ago))
            else relativeDays(-ago).replaceFirstChar { it.uppercase() }
        }
        else -> stringResource(R.string.part_not_yet)
    }
}

private const val MONTHS_FROM = 60L

/**
 * A tracker's parts as rows on its card: each part's name, where it stands, and a button that
 * logs it with that part already picked. A due part reads in the error color.
 */
@Composable
fun PartRows(
    parts: List<PartStatus>,
    tracker: Tracker,
    onLog: (String) -> Unit,
    modifier: Modifier = Modifier,
    rowColor: Color = MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.7f),
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        parts.forEachIndexed { index, part ->
            // Connected rows: the group's outer corners round, the inner ones stay tight.
            val top = if (index == 0) 16.dp else 4.dp
            val bottom = if (index == parts.lastIndex) 16.dp else 4.dp
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(top, top, bottom, bottom))
                    .background(rowColor)
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(part.label, style = MaterialTheme.typography.titleSmall, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        partText(part, tracker),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (part.due) colors.error else colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledTonalIconButton(shapes = IconButtonDefaults.shapes(), onClick = { onLog(part.optionId) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.part_log, part.label), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
