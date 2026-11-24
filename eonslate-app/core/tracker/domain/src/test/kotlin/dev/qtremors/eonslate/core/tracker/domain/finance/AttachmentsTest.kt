package dev.qtremors.eonslate.core.tracker.domain.finance

import dev.qtremors.eonslate.core.tracker.domain.backup.BackupCheck
import dev.qtremors.eonslate.core.tracker.domain.backup.BackupSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentsTest {
    private val hash = "a".repeat(64)

    @Test
    fun aTypedAddressBecomesALink() {
        assertEquals("https://amazon.in/dp/B0C", Attachments.link("  amazon.in/dp/B0C "))
        assertEquals("http://shop.example.com/x?y=1", Attachments.link("http://shop.example.com/x?y=1"))
        assertNull(Attachments.link("not a link"))
        assertNull(Attachments.link("ftp://files.example.com/a"))
        assertNull(Attachments.link("javascript:alert(1)"))
        assertNull(Attachments.link("nodot"))
        assertNull(Attachments.link(""))
        assertEquals("amazon.in", Attachments.host("https://www.amazon.in/dp/B0C"))
    }

    @Test
    fun filesAreNamedByTheirHashAndLinksByTheirAddress() {
        assertTrue(Attachment("a", "t", AttachmentKind.IMAGE, hash).isValid())
        assertTrue(Attachment("b", "t", AttachmentKind.LINK, "https://amazon.in/dp/B0C", "Product page").isValid())
        assertFalse(Attachment("c", "t", AttachmentKind.PDF, "../../etc/passwd").isValid())
        // A link is stored as it will be opened, never as typed.
        assertFalse(Attachment("d", "t", AttachmentKind.LINK, "amazon.in/dp/B0C").isValid())
        assertFalse(Attachment("e", "t", AttachmentKind.LINK, "https://a.in", "x".repeat(Attachment.NAME_LIMIT + 1)).isValid())
        assertEquals(mapOf(hash to AttachmentKind.IMAGE), Attachments.files(listOf(Attachment("a", "t", AttachmentKind.IMAGE, hash))))
    }

    @Test
    fun aBackupsAttachmentsBelongToItsTransactions() {
        val bank = Account("bank", "HDFC", "INR")
        val snapshot = BackupSnapshot(
            schema = 21, createdAt = 0, appVersion = "t",
            accounts = listOf(bank),
            transactions = listOf(Transaction("t", "bank", TransactionKind.EXPENSE, 100, 1)),
            attachments = listOf(
                Attachment("ok", "t", AttachmentKind.IMAGE, hash),
                Attachment("lost", "gone", AttachmentKind.IMAGE, hash),
            ),
        )
        val problems = BackupCheck.problems(snapshot, currentSchema = 21)
        assertTrue(problems.toString(), problems.any { "invalid attachment lost" in it })
        assertFalse(problems.toString(), problems.any { "invalid attachment ok" in it })
    }
}
