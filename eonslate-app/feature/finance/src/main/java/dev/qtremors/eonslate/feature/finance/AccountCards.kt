package dev.qtremors.eonslate.feature.finance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.AccountKind
import dev.qtremors.eonslate.core.tracker.domain.finance.DepositStatus
import dev.qtremors.eonslate.core.tracker.ui.displayDate
import androidx.compose.material.icons.rounded.LockClock
import dev.qtremors.eonslate.core.tracker.ui.ValueFormat
import dev.qtremors.eonslate.core.ui.components.BadgePalette
import dev.qtremors.eonslate.core.ui.components.FitText
import dev.qtremors.eonslate.core.ui.theme.LocalValueDisplay
import dev.qtremors.eonslate.core.ui.theme.MetricTextStyle
import java.math.BigInteger

/** The icon for what an account is. */
internal fun AccountKind.icon(): ImageVector = when (this) {
    AccountKind.BANK -> Icons.Rounded.AccountBalance
    AccountKind.CARD -> Icons.Rounded.CreditCard
    AccountKind.WALLET -> Icons.Rounded.AccountBalanceWallet
    AccountKind.CASH -> Icons.Rounded.Payments
    AccountKind.DEPOSIT -> Icons.Rounded.LockClock
}

@Composable
internal fun AccountKind.label(): String = stringResource(
    when (this) {
        AccountKind.BANK -> R.string.account_kind_bank
        AccountKind.CARD -> R.string.account_kind_card
        AccountKind.WALLET -> R.string.account_kind_wallet
        AccountKind.CASH -> R.string.account_kind_cash
        AccountKind.DEPOSIT -> R.string.account_kind_deposit
    },
)

/**
 * Every account as a card, swiped side to side: what it is, its name, its balance in its own
 * currency, and its last digits. A running fixed deposit shows what it is worth today instead,
 * growing day by day; closed ones are left out. A tap opens the account; the last card adds one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountCarousel(
    accounts: List<Account>,
    balances: Map<String, BigInteger>,
    onAccount: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    setAside: Map<String, BigInteger> = emptyMap(),
    deposits: Map<String, DepositStatus> = emptyMap(),
) {
    val shown = remember(accounts) { accounts.filterNot { it.isClosed } }
    val state = rememberCarouselState { shown.size + 1 }
    HorizontalUncontainedCarousel(
        state = state,
        itemWidth = CARD_WIDTH,
        itemSpacing = 12.dp,
        modifier = modifier.fillMaxWidth().height(CARD_HEIGHT),
    ) { index ->
        val shape = RoundedCornerShape(28.dp)
        val account = shown.getOrNull(index)
        Box(Modifier.fillMaxSize().maskClip(shape)) {
            if (account == null) AddCard(onAdd)
            else AccountCard(account, balances[account.id] ?: BigInteger.ZERO, setAside[account.id], deposits[account.id]) { onAccount(account.id) }
        }
    }
}

@Composable
private fun AccountCard(account: Account, balance: BigInteger, aside: BigInteger?, deposit: DepositStatus?, onClick: () -> Unit) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val tone = remember(account.id, dark) { BadgePalette.forKey(account.id, dark) }
    val locale = LocalValueDisplay.current.numberLocale
    val kind = account.kind.label()
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(tone.container, lerp(tone.container, tone.content, 0.22f))))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.account_card_open), onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(account.kind.icon(), contentDescription = kind, tint = tone.content, modifier = Modifier.size(22.dp))
            Text(
                account.name,
                style = MaterialTheme.typography.titleMedium,
                color = tone.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.weight(1f))
        FitText(ValueFormat.money(deposit?.worth ?: balance, account.currency, locale), style = MetricTextStyle, color = tone.content)
        deposit?.let {
            // What it has earned once it has earned anything, then what it pays at maturity.
            Text(
                listOfNotNull(
                    it.earned.takeIf { e -> e.signum() > 0 }?.let { e -> stringResource(R.string.deposit_card_earned, ValueFormat.money(e, account.currency, locale)) },
                    stringResource(R.string.deposit_on_day, ValueFormat.money(it.atMaturity, account.currency, locale), displayDate(it.deposit.maturityEpochDay)),
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelMedium,
                color = tone.content.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        aside?.takeIf { it.signum() > 0 }?.let {
            Text(
                stringResource(R.string.piggy_free_line, ValueFormat.money(balance - it, account.currency, locale)),
                style = MaterialTheme.typography.labelMedium,
                color = tone.content.copy(alpha = 0.8f),
            )
        }
        account.floorMinor?.let { floor ->
            val under = BigInteger.valueOf(floor) - balance
            Text(
                if (under.signum() > 0) stringResource(R.string.floor_card_under, ValueFormat.money(under, account.currency, locale))
                else stringResource(R.string.floor_card_keep, ValueFormat.money(floor, account.currency, locale)),
                style = MaterialTheme.typography.labelMedium,
                color = if (under.signum() > 0) MaterialTheme.colorScheme.error else tone.content.copy(alpha = 0.8f),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(kind, style = MaterialTheme.typography.labelLarge, color = tone.content.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
            account.digits?.let {
                Text("•••• $it", style = MaterialTheme.typography.titleSmall, color = tone.content)
            }
            account.deposit?.let {
                Text(rateText(it.rateBp, locale), style = MaterialTheme.typography.titleSmall, color = tone.content)
            }
        }
    }
}

@Composable
private fun AddCard(onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .border(BorderStroke(1.dp, colors.outlineVariant), RoundedCornerShape(28.dp))
            .clickable(role = Role.Button, onClick = onAdd)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.primary)
        Text(stringResource(R.string.account_card_add), style = MaterialTheme.typography.titleSmall, color = colors.primary)
    }
}

private val CARD_WIDTH = 280.dp
private val CARD_HEIGHT = 168.dp
