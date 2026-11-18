package dev.qtremors.eonslate.core.tracker.domain.csv

import dev.qtremors.eonslate.core.tracker.domain.finance.Account
import dev.qtremors.eonslate.core.tracker.domain.finance.Ledger
import dev.qtremors.eonslate.core.tracker.domain.finance.Transaction
import dev.qtremors.eonslate.core.tracker.domain.finance.TransactionKind
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.value.EpochDays
import dev.qtremors.eonslate.core.tracker.domain.value.MoneyAmount
import java.io.Writer
import java.util.Locale

object FinanceCsv {
    private val columns = listOf(
        "date", "kind", "amount", "currency", "account", "account_id", "destination", "destination_id", "category", "note",
    )
    private val legacyColumns = listOf("date", "description", "category", "type", "amount", "notes")

    fun write(ledger: Ledger, output: Writer) {
        val accounts = ledger.accounts.associateBy { it.id }
        Csv.writeRow(output, columns)
        for (entry in ledger.transactions) {
            val source = accounts.getValue(entry.accountId)
            Csv.writeRow(
                output,
                listOf(
                    EpochDays.format(entry.epochDay), entry.kind.name.lowercase(Locale.ROOT),
                    MoneyAmount.format(entry.minor, source.currency), source.currency, Csv.escapeText(source.name), Csv.escapeText(source.id),
                    Csv.escapeText(entry.destinationId?.let { accounts.getValue(it).name }.orEmpty()), Csv.escapeText(entry.destinationId.orEmpty()),
                    Csv.escapeText(entry.category), Csv.escapeText(entry.note),
                ),
            )
        }
    }

    /** Older transaction exports carry no account or currency; the selected account supplies both. */
    fun read(source: String, accounts: List<Account>, fallback: Account, ids: IdGenerator): List<Transaction> {
        val rows = Csv.rows(source).iterator()
        if (!rows.hasNext()) throw CsvException(CsvProblem.COLUMNS)
        val header = rows.next().map { it.trim().lowercase(Locale.ROOT) }
        val legacy = header.size == legacyColumns.size && header.toSet() == legacyColumns.toSet()
        val expected = if (legacy) legacyColumns else columns
        if (header.size != expected.size || header.toSet() != expected.toSet()) throw CsvException(CsvProblem.COLUMNS)
        val order = expected.map(header::indexOf)
        val byId = accounts.associateBy { it.id }
        val byName = accounts.groupBy { it.currency to it.name.lowercase(Locale.ROOT) }
        val entries = mutableListOf<Transaction>()
        var row = 1
        while (rows.hasNext()) {
            val cells = rows.next()
            row++
            if (cells.size == 1 && cells[0].isBlank()) continue
            if (entries.size >= TrackerCsv.MAX_ENTRIES) throw CsvException(CsvProblem.TOO_LARGE)
            if (cells.size != expected.size) throw CsvException(CsvProblem.COLUMNS, row)
            val values = order.map(cells::get)
            val entry = if (legacy) readLegacy(values, fallback, ids, row) else readEntry(values, byId, byName, ids, row)
            if (!entry.isValid(byId)) throw CsvException(CsvProblem.ENTRY, row)
            entries += entry
        }
        return entries
    }

    private fun readEntry(
        fields: List<String>,
        byId: Map<String, Account>,
        byName: Map<Pair<String, String>, List<Account>>,
        ids: IdGenerator,
        row: Int,
    ): Transaction {
        val (date, kind, amount, currency) = fields
        fun account(name: String, id: String) =
            byId[Csv.unescapeText(id)]?.takeIf { it.currency == currency }
                ?: byName[currency to Csv.unescapeText(name).lowercase(Locale.ROOT)]?.singleOrNull()
        val from = account(fields[4], fields[5]) ?: throw CsvException(CsvProblem.ENTRY, row)
        val to = if (fields[6].isBlank() && fields[7].isBlank()) null
        else account(fields[6], fields[7]) ?: throw CsvException(CsvProblem.ENTRY, row)
        return Transaction(
            ids.newId(), from.id, kind(kind, row), minor(amount, currency, row), day(date, row),
            Csv.unescapeText(fields[9]), to?.id, Csv.unescapeText(fields[8]),
        )
    }

    private fun readLegacy(fields: List<String>, account: Account, ids: IdGenerator, row: Int): Transaction {
        val (date, description, category, kind, amount) = fields
        val note = listOf(description, fields[5]).filter { it.isNotBlank() }.joinToString(" · ")
        return Transaction(
            ids.newId(), account.id, kind(kind, row), minor(amount, account.currency, row), day(date, row),
            note = note, category = category,
        )
    }

    private fun kind(text: String, row: Int): TransactionKind =
        TransactionKind.entries.firstOrNull { it.name.equals(text.trim(), ignoreCase = true) }
            ?: throw CsvException(CsvProblem.ENTRY, row)

    private fun day(text: String, row: Int): Long = EpochDays.parse(text) ?: throw CsvException(CsvProblem.ENTRY, row)

    private fun minor(text: String, currency: String, row: Int): Long {
        val decimal = text.trim().takeIf { it.length <= 40 }?.toBigDecimalOrNull() ?: throw CsvException(CsvProblem.ENTRY, row)
        if (decimal.scale() !in -20..40) throw CsvException(CsvProblem.ENTRY, row)
        return try {
            decimal.movePointRight(MoneyAmount.fractionDigits(currency)).longValueExact()
        } catch (_: ArithmeticException) {
            throw CsvException(CsvProblem.ENTRY, row)
        }
    }
}
