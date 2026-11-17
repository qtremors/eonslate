package dev.qtremors.eonslate.feature.backup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.qtremors.eonslate.core.tracker.domain.backup.DataCounts
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class BackupUiTest {
    @get:Rule
    val compose = createComposeRule()

    private var created: String? = null
    private var picked = 0
    private var restored = 0
    private var undone = 0

    /** Holds the state like the view model does, so a shown message is really cleared. */
    private fun show(initial: BackupUiState) = compose.setContent {
        var state by remember { mutableStateOf(initial) }
        EonslateTheme {
            BackupContent(
                state = state,
                onBack = {},
                onCreate = { created = String(it) },
                onPickBackup = { picked++ },
                onUnlock = {},
                onRestore = { restored++ },
                onPutAway = {},
                onUndo = { undone++ },
                onMessageShown = { state = state.copy(message = null) },
            )
        }
    }

    @Test
    fun aMessageStaysUpUntilItIsShown() {
        show(BackupUiState(counts = DataCounts(1, 1), message = BackupMessage.NOT_A_BACKUP))
        compose.onNodeWithText("Not an Eonslate backup").assertIsDisplayed()
    }

    @Test
    fun aRestoreCanBeUndoneFromItsSnackbar() {
        show(BackupUiState(counts = DataCounts(1, 1), message = BackupMessage.RESTORED))
        compose.onNodeWithText("Restored").assertIsDisplayed()
        compose.onNode(hasText("Undo") and hasClickAction()).performClick()
        assertEquals(1, undone)
    }

    @Test
    fun thePageSaysWhatIsHere() {
        show(BackupUiState(counts = DataCounts(3, 1)))
        compose.onNodeWithText("On this phone: 3 trackers, 1 entry").assertIsDisplayed()
        compose.onNodeWithText("Restore from a backup").performClick()
        assertEquals(1, picked)
        assertEquals(0, compose.onAllNodesWithText("Undo last restore").fetchSemanticsNodes().size)
    }

    @Test
    fun aShortOrMismatchedPasswordIsRefused() {
        show(BackupUiState(counts = DataCounts(1, 1)))
        compose.onNodeWithText("Create a backup").performClick()
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("short")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("At least 8 characters").assertIsDisplayed()
        assertNull(created)

        fields[0].performTextInput(" enough")
        fields[1].performTextInput("different")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Passwords don't match").assertIsDisplayed()
        assertNull(created)
    }

    @Test
    fun matchingPasswordsGoOnToThePicker() {
        show(BackupUiState(counts = DataCounts(1, 1)))
        compose.onNodeWithText("Create a backup").performClick()
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("long enough")
        fields[1].performTextInput("long enough")
        compose.onNodeWithText("Save").performClick()
        assertEquals("long enough", created)
    }

    @Test
    fun withoutEncryptionNoPasswordIsNeeded() {
        show(BackupUiState(counts = DataCounts(1, 1)))
        compose.onNodeWithText("Create a backup").performClick()
        compose.onNodeWithText("Encrypt").performClick()
        compose.onNodeWithText("Save").performClick()
        assertEquals("", created)
    }

    @Test
    fun theReviewComparesTheBackupWithThePhone() {
        val snapshot = snapshotOf(4, 9)
        val summary = snapshot.copy(trackers = snapshot.trackers.mapIndexed { i, t -> t.copy(archived = i == 0) }).summary
        val current = DataCounts(2, 5, accounts = 1, transactions = 2)
        show(BackupUiState(counts = current, review = Review(summary, current)))
        compose.onNodeWithText("4 trackers, 9 entries").assertIsDisplayed()
        compose.onNodeWithText("1 archived").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Replaces 2 trackers, 5 entries, 1 account, 2 transactions", substring = true).fetchSemanticsNodes().isNotEmpty())
        // The section header says "Restore" too; the button is the one that can be pressed.
        compose.onNode(hasText("Restore") and hasClickAction()).performClick()
        assertEquals(1, restored)
    }

    @Test
    fun aRestorePointCanBeUndoneAfterAsking() {
        show(BackupUiState(counts = DataCounts(4, 9), restorePoint = 1_762_740_000_000L))
        compose.onNodeWithText("Undo last restore").performClick()
        compose.onNodeWithText("Changes since the restore will be lost.").assertIsDisplayed()
        compose.onNode(hasText("Undo") and hasClickAction()).performClick()
        assertEquals(1, undone)
    }

    @Test
    fun rowsWaitWhileWorkIsRunning() {
        show(BackupUiState(counts = DataCounts(1, 1), busy = Busy.CREATING))
        compose.onNodeWithText("Creating backup…").assertIsDisplayed()
        compose.onNodeWithText("Restore from a backup").assertIsNotEnabled()
    }
}
