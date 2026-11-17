package dev.qtremors.eonslate.feature.finance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessage
import dev.qtremors.eonslate.core.tracker.domain.finance.PaymentMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

internal object PaymentInbox {
    @Suppress("DEPRECATION")
    fun available(context: Context): Boolean =
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions?.contains(Manifest.permission.READ_SMS) == true

    fun hasAccess(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    suspend fun scan(context: Context, now: Long, zone: ZoneId, days: Int): List<PaymentMessage> = withContext(Dispatchers.IO) {
        if (!hasAccess(context)) return@withContext emptyList()
        val cutoff = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().minusDays(days.toLong() - 1)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        val found = linkedMapOf<String, PaymentMessage>()
        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "${Telephony.Sms.DATE} >= ?",
            arrayOf(cutoff.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { cursor ->
            while (cursor.moveToNext() && found.size < 200) {
                ensureActive()
                val text = cursor.getString(1) ?: continue
                val day = Instant.ofEpochMilli(cursor.getLong(2)).atZone(zone).toLocalDate().toEpochDay()
                val payment = PaymentMessages.parse(text, cursor.getString(0), day) ?: continue
                payment.key?.let { found.putIfAbsent(it, payment) }
            }
        }
        found.values.toList()
    }
}
