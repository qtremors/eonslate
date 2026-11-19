package dev.qtremors.eonslate.feature.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.qtremors.eonslate.core.ui.components.ConnectedGroup
import dev.qtremors.eonslate.core.ui.components.ConnectedRow
import dev.qtremors.eonslate.core.ui.components.EonslateScaffold
import dev.qtremors.eonslate.core.ui.components.IconBadge
import dev.qtremors.eonslate.core.ui.components.NavigationRow
import dev.qtremors.eonslate.core.ui.components.SectionHeader
import dev.qtremors.eonslate.core.ui.R as UiR

/** Where Eonslate lives online. Opening one leaves the app for the browser. */
object Links {
    const val DEVELOPER = "https://github.com/qtremors"
    const val SOURCE = "https://github.com/qtremors/eonslate"
    const val RELEASES = "https://github.com/qtremors/eonslate/releases"
    const val ISSUES = "https://github.com/qtremors/eonslate/issues"
}

/**
 * About Eonslate: the mark, name, version and tagline, then the project's links, the legal
 * pages, and this device's details for bug reports.
 */
@Composable
fun AboutScreen(
    versionName: String,
    onBack: () -> Unit,
    onLicenses: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val device = deviceLine()
    EonslateScaffold(title = stringResource(R.string.about_title), onBack = onBack, modifier = modifier) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Image(
                        painter = painterResource(UiR.drawable.eonslate_mark),
                        contentDescription = null,
                        modifier = Modifier.size(88.dp)
                            .background(Color.White, RoundedCornerShape(24.dp)),
                    )
                    Text(stringResource(R.string.about_app_name), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.about_version, versionName), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(R.string.about_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item(key = "project") {
                SectionHeader(stringResource(R.string.about_section_project))
                ConnectedGroup {
                    NavigationRow(stringResource(R.string.about_developer), Icons.Rounded.Person, "developer", onClick = { open(context, Links.DEVELOPER) }, supporting = stringResource(R.string.about_developer_name))
                    NavigationRow(stringResource(R.string.about_source), Icons.Rounded.Code, "source", onClick = { open(context, Links.SOURCE) })
                    NavigationRow(stringResource(R.string.about_releases), Icons.Rounded.NewReleases, "releases", onClick = { open(context, Links.RELEASES) })
                    NavigationRow(stringResource(R.string.about_issues), Icons.Rounded.BugReport, "issues", onClick = { open(context, Links.ISSUES) })
                }
            }
            item(key = "legal") {
                SectionHeader(stringResource(R.string.about_section_legal))
                ConnectedGroup {
                    NavigationRow(stringResource(R.string.privacy_title), Icons.Rounded.Shield, "privacy", onClick = onPrivacy, supporting = stringResource(R.string.about_privacy_hint))
                    ConnectedRow(
                        title = stringResource(R.string.about_license),
                        supporting = stringResource(R.string.about_license_name),
                        leading = { IconBadge(icon = Icons.Rounded.Gavel, key = "license") },
                    )
                    NavigationRow(stringResource(R.string.licenses_title), Icons.AutoMirrored.Rounded.Article, "licenses", onClick = onLicenses)
                }
            }
            item(key = "device") {
                SectionHeader(stringResource(R.string.about_section_device))
                ConnectedGroup {
                    ConnectedRow(
                        title = device,
                        supporting = stringResource(R.string.about_device_hint),
                        leading = { IconBadge(icon = Icons.Rounded.PhoneAndroid, key = "device") },
                        onClick = { copy(context, "Eonslate $versionName · $device") },
                    )
                }
            }
        }
    }
}

/** Maker, model, Android version and API level, such as "motorola moto g(40) fusion · Android 12 (API 31)". */
@Composable
private fun deviceLine(): String = stringResource(R.string.about_device, Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE, Build.VERSION.SDK_INT)

internal fun open(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No browser: hand the link over instead, so it can still be opened elsewhere.
        copy(context, url)
    }
}

internal fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Eonslate", text))
    // Android 13 and up show their own copied notice; older versions get one from the app.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.about_copied, Toast.LENGTH_SHORT).show()
    }
}
