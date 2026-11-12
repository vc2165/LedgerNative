package com.vinay.ledger.data

import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/* ---------- Import: rows pasted from Excel ("date<TAB>description<TAB>amount") ---------- */

data class PastedRow(val date: LocalDate, val desc: String, val amount: Double, val rule: String)
data class PasteResult(val rows: List<PastedRow>, val skipped: List<String>)

private fun parseDmy(s: String): LocalDate? {
    val m = Regex("""^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})$""").find(s.trim()) ?: return null
    var y = m.groupValues[3].toInt()
    if (y < 100) y += 2000
    return try { LocalDate.of(y, m.groupValues[2].toInt(), m.groupValues[1].toInt()) } catch (e: Exception) { null }
}

fun parsePaste(text: String): PasteResult {
    val out = ArrayList<PastedRow>()
    val skipped = ArrayList<String>()
    var lastDate: LocalDate? = null
    var lastCat: String? = null
    for (raw in text.split(Regex("\\r?\\n"))) {
        if (raw.isBlank()) continue
        var cols = raw.split('\t')
        if (cols.size < 2) cols = raw.split(Regex(""",(?=(?:[^"]*"[^"]*")*[^"]*$)"""))
        val c = cols.map { it.trim().removePrefix("\"").removeSuffix("\"") }.toMutableList()
        while (c.isNotEmpty() && c.last().isEmpty()) c.removeAt(c.size - 1)
        if (c.size < 2) {
            if (!Regex("""^[a-z]+[_ -]?\d{4}$""", RegexOption.IGNORE_CASE).matches(raw.trim())) skipped.add(raw)
            continue
        }
        val amt = parseAmount(c.last())
        if (amt == null || amt <= 0) { skipped.add(raw); continue }
        val dt = parseDmy(c[0])
        if (dt != null) lastDate = dt
        val date = lastDate
        if (date == null) { skipped.add(raw); continue }
        val desc = (if (dt != null) c.subList(1, c.size - 1) else c.subList(0, c.size - 1)).filter { it.isNotEmpty() }.joinToString(" ").trim()
        val cat = if (desc.isNotEmpty()) categorize(desc) else (lastCat ?: "other")
        lastCat = cat
        out.add(PastedRow(date, desc.ifEmpty { "(unlabelled)" }, r2(amt), cat))
    }
    return PasteResult(out, skipped)
}

/* ---------- Export: a real .xlsx built offline ---------- */

private sealed class Cell {
    data class T(val s: String) : Cell()
    data class N(val v: Double) : Cell()
    data class D(val iso: String) : Cell()
}

private fun t(s: String) = Cell.T(s)
private fun n(v: Double) = Cell.N(v)
private fun n(v: Int) = Cell.N(v.toDouble())
private fun d(iso: String) = Cell.D(iso)

private fun xesc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

private fun colName(i0: Int): String {
    var i = i0 + 1
    val sb = StringBuilder()
    while (i > 0) { val m = (i - 1) % 26; sb.insert(0, ('A' + m)); i = (i - 1) / 26 }
    return sb.toString()
}

private fun sheetXml(rows: List<List<Cell>>, widths: List<Int>): String {
    val sb = StringBuilder()
    sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews><cols>""")
    widths.forEachIndexed { i, w -> sb.append("""<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1"/>""") }
    sb.append("</cols><sheetData>")
    val epoch = LocalDate.of(1899, 12, 30)
    rows.forEachIndexed { ri, r ->
        sb.append("""<row r="${ri + 1}">""")
        r.forEachIndexed { ci, v ->
            val ref = colName(ci) + (ri + 1)
            val style = if (ri == 0) " s=\"1\"" else ""
            when (v) {
                is Cell.D -> sb.append("""<c r="$ref" s="2"><v>${ChronoUnit.DAYS.between(epoch, LocalDate.parse(v.iso))}</v></c>""")
                is Cell.N -> sb.append("""<c r="$ref" s="${if (ri == 0) 1 else 3}"><v>${v.v}</v></c>""")
                is Cell.T -> sb.append("""<c r="$ref" t="inlineStr"$style><is><t>${xesc(v.s)}</t></is></c>""")
            }
        }
        sb.append("</row>")
    }
    sb.append("</sheetData></worksheet>")
    return sb.toString()
}

private fun workbook(names: List<String>, sheets: List<List<List<Cell>>>, widths: List<List<Int>>): ByteArray {
    val files = ArrayList<Pair<String, String>>()
    files += "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""" +
        names.indices.joinToString("") { """<Override PartName="/xl/worksheets/sheet${it + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" } + "</Types>"
    files += "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
    files += "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""" +
        names.mapIndexed { i, nm -> """<sheet name="${xesc(nm)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""" }.joinToString("") + "</sheets></workbook>"
    files += "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
        names.indices.joinToString("") { """<Relationship Id="rId${it + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${it + 1}.xml"/>""" } +
        """<Relationship Id="rId${names.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>"""
    files += "xl/styles.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><numFmts count="2"><numFmt numFmtId="164" formatCode="dd/mm/yyyy"/><numFmt numFmtId="165" formatCode="#,##0.00"/></numFmts><fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FFF3F4F6"/></patternFill></fill></fills><borders count="1"><border/></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="4"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/><xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/><xf numFmtId="165" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""
    sheets.forEachIndexed { i, s -> files += "xl/worksheets/sheet${i + 1}.xml" to sheetXml(s, widths[i]) }
    val bos = ByteArrayOutputStream()
    ZipOutputStream(bos).use { z ->
        for ((name, content) in files) {
            z.putNextEntry(ZipEntry(name))
            z.write(content.toByteArray(Charsets.UTF_8))
            z.closeEntry()
        }
    }
    return bos.toByteArray()
}

private fun LedgerData.expenseRows(es: List<Entry>): List<List<Cell>> =
    listOf(listOf(t("Date"), t("Description"), t("Category"), t("Amount (₹)"), t("Type (if set)"))) +
        es.map { e -> listOf(d(e.date), t(e.desc), t(cat(e.cat).name), n(e.amount), t(e.bucket?.label ?: "")) }

fun LedgerData.buildAllXlsx(): ByteArray {
    val es = entries.sortedWith(compareBy<Entry>({ it.date }, { it.created }))
    val ms = monthsWithData()
    val st = ms.map { stats(it) }
    val s2 = listOf(listOf("Month", "Total spent", "Fixed", "Recurring", "One-offs", "Food delivery", "Delivery orders", "Salary", "Saved / invested", "Left after savings and bills").map { t(it) }) +
        ms.indices.map { i ->
            val s = st[i]
            listOf(t(mFull(ms[i])), n(r2(s.total)), n(r2(s.b[Bucket.FIXED]!!)), n(r2(s.b[Bucket.RECURRING]!!)), n(r2(s.b[Bucket.ONEOFF]!!)),
                n(r2(s.delivSum)), n(s.delivN), n(s.inc.salary), n(s.saved), n(r2(s.net)))
        }
    val ids = cats.keys.filter { c -> entries.any { resolve(it.cat) == c } }
    val s3 = listOf(listOf(t("Category")) + ms.map { t(mFull(it)) }) + ids.map { c -> listOf(t(cats[c]!!.name)) + st.map { n(r2(it.by[c] ?: 0.0)) } }
    return workbook(
        listOf("Expenses", "Monthly summary", "By category"), listOf(expenseRows(es), s2, s3),
        listOf(listOf(12, 34, 26, 13, 13), listOf(18, 13, 11, 12, 11, 13, 15, 11, 15, 17), listOf(30) + ms.map { 15 }),
    )
}

fun LedgerData.buildMonthXlsx(k: String): ByteArray {
    val st = stats(k)
    val tot = st.total
    val es = st.es.sortedWith(compareBy<Entry>({ it.date }, { it.created }))
    val sum = ArrayList<List<Cell>>()
    sum += listOf(t("Item"), t("Amount (₹)"))
    sum += listOf(t("Salary"), n(st.inc.salary))
    st.inc.saves.forEach { sum += listOf(t(it.name), n(it.amount)) }
    sum += listOf(t("Total saved / invested"), n(st.saved))
    sum += listOf(t("Total spent"), n(r2(tot)))
    st.pendingList.forEach { sum += listOf(t("${it.name} (bill, not paid yet)"), n(it.amount)) }
    sum += listOf(t(if (st.bills.isNotEmpty()) "Left after savings and bills" else "Left after savings"), n(r2(st.net)))
    sum += listOf(t(""), t(""))
    sum += listOf(t("Fixed"), n(r2(st.b[Bucket.FIXED]!!)))
    sum += listOf(t("Recurring"), n(r2(st.b[Bucket.RECURRING]!!)))
    sum += listOf(t("One-offs"), n(r2(st.b[Bucket.ONEOFF]!!)))
    sum += listOf(t(""), t(""))
    sum += listOf(t("Food delivery orders"), n(st.delivN))
    sum += listOf(t("Food delivery total"), n(r2(st.delivSum)))
    sum += listOf(t("Average order"), n(if (st.delivN > 0) r2(st.delivSum / st.delivN) else 0.0))
    val catsSorted = st.by.entries.sortedByDescending { it.value }
    val catRows = listOf(listOf(t("Category"), t("Entries"), t("Amount (₹)"), t("Share %"))) +
        catsSorted.map { (c, v) -> listOf(t(cat(c).name), n(st.cnt[c] ?: 0), n(r2(v)), n(Math.round(v / tot * 1000) / 10.0)) } +
        listOf(listOf(t("Total"), n(st.es.size), n(r2(tot)), n(100)))
    val one = es.filter { bucketOf(it) == Bucket.ONEOFF }.sortedByDescending { it.amount }
    val ones = listOf(listOf(t("Item"), t("Date"), t("Amount (₹)"))) + one.map { listOf(t(it.desc), d(it.date), n(it.amount)) } +
        listOf(listOf(t("Total"), t(""), n(r2(st.b[Bucket.ONEOFF]!!))))
    val wk = listOf(listOf(t("Week"), t("Recurring (₹)"), t("Of which delivery (₹)"))) +
        st.weeks.map { listOf(t("${it.a}–${it.z} ${mShort(k)}"), n(r2(it.rec)), n(r2(it.del))) } +
        listOf(listOf(t("Total"), n(r2(st.b[Bucket.RECURRING]!!)), n(r2(st.delivSum))))
    return workbook(
        listOf("Summary", "Expenses", "Categories", "One-offs", "Week by week"),
        listOf(sum, expenseRows(es), catRows, ones, wk),
        listOf(listOf(26, 16), listOf(12, 34, 26, 13, 13), listOf(30, 10, 14, 10), listOf(34, 12, 14), listOf(18, 16, 22)),
    )
}
