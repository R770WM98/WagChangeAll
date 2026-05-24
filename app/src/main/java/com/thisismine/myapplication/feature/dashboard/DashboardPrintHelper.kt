package com.thisismine.myapplication.feature.dashboard

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import com.thisismine.myapplication.domain.model.MaintenanceEntry
import com.thisismine.myapplication.domain.model.PartReplacement

fun printServiceHistory(
    context: Context,
    motorcycleName: String,
    entries: List<MaintenanceEntry>
) {
    if (entries.isEmpty()) return

    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val webView = WebView(context)
    val html = buildServiceHistoryHtml(motorcycleName, entries)
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    val jobName = "${motorcycleName.takeIf { it.isNotBlank() } ?: "Motorcycle"} Service History"
    printManager.print(
        jobName,
        webView.createPrintDocumentAdapter(jobName),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
    )
}

fun printPartHistory(
    context: Context,
    motorcycleName: String,
    entries: List<PartReplacement>
) {
    if (entries.isEmpty()) return

    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val webView = WebView(context)
    val html = buildPartHistoryHtml(motorcycleName, entries)
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    val jobName = "${motorcycleName.takeIf { it.isNotBlank() } ?: "Motorcycle"} Parts History"
    printManager.print(
        jobName,
        webView.createPrintDocumentAdapter(jobName),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
    )
}

fun printCombinedHistory(
    context: Context,
    motorcycleName: String,
    maintenanceEntries: List<MaintenanceEntry>,
    partReplacements: List<PartReplacement>
) {
    if (maintenanceEntries.isEmpty() && partReplacements.isEmpty()) return

    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val webView = WebView(context)
    val html = buildFullMaintenanceHistoryHtml(motorcycleName, maintenanceEntries, partReplacements)
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    val jobName = "${motorcycleName.takeIf { it.isNotBlank() } ?: "Motorcycle"} Maintenance & Parts History"
    printManager.print(
        jobName,
        webView.createPrintDocumentAdapter(jobName),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
    )
}

internal fun buildServiceHistoryHtml(
    motorcycleName: String,
    entries: List<MaintenanceEntry>
): String {
    val rows = entries.joinToString(separator = "") { entry ->
        """
        <tr>
            <td>${entry.dateIso.htmlEscape()}</td>
            <td>${entry.title.htmlEscape()}</td>
            <td>${entry.odometerKm}</td>
            <td>PHP ${"%.2f".format(entry.totalCostPhp)}</td>
            <td>${entry.notes.htmlEscape()}</td>
        </tr>
        """.trimIndent()
    }

    val displayName = motorcycleName.ifBlank { "Motorcycle" }.htmlEscape()

    return """
        <html>
        <head>
            <meta charset="utf-8" />
            <style>
                body { font-family: sans-serif; margin: 24px; }
                h1 { margin: 0 0 4px 0; font-size: 20px; }
                p { margin: 0 0 16px 0; color: #666; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #ddd; padding: 8px; vertical-align: top; }
                th { background: #f5f5f5; text-align: left; }
            </style>
        </head>
        <body>
            <h1>Service History</h1>
            <p>$displayName</p>
            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Service</th>
                        <th>Odometer (km)</th>
                        <th>Cost</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
        </body>
        </html>
    """.trimIndent()
}

internal fun buildPartHistoryHtml(
    motorcycleName: String,
    entries: List<PartReplacement>
): String {
    val rows = entries.joinToString(separator = "") { entry ->
        """
        <tr>
            <td>${entry.dateIso.htmlEscape()}</td>
            <td>${entry.partName.htmlEscape()}</td>
            <td>${entry.odometerKm}</td>
            <td>PHP ${"%.2f".format(entry.totalCostPhp)}</td>
            <td>${entry.notes.htmlEscape()}</td>
        </tr>
        """.trimIndent()
    }

    val displayName = motorcycleName.ifBlank { "Motorcycle" }.htmlEscape()

    return """
        <html>
        <head>
            <meta charset="utf-8" />
            <style>
                body { font-family: sans-serif; margin: 24px; }
                h1 { margin: 0 0 4px 0; font-size: 20px; }
                p { margin: 0 0 16px 0; color: #666; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #ddd; padding: 8px; vertical-align: top; }
                th { background: #f5f5f5; text-align: left; }
            </style>
        </head>
        <body>
            <h1>Parts Replacement History</h1>
            <p>$displayName</p>
            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Part</th>
                        <th>Odometer (km)</th>
                        <th>Cost</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
        </body>
        </html>
    """.trimIndent()
}

internal fun buildFullMaintenanceHistoryHtml(
    motorcycleName: String,
    maintenanceEntries: List<MaintenanceEntry>,
    partReplacements: List<PartReplacement>
): String {
    // Create a combined list with type information and sort by date
    data class HistoryEvent(
        val date: String,
        val description: String,
        val odometer: Int,
        val cost: Double,
        val notes: String
    )

    val events = mutableListOf<HistoryEvent>()

    // Add all maintenance entries
    maintenanceEntries.forEach { entry ->
        events.add(
            HistoryEvent(
                date = entry.dateIso,
                description = entry.title,
                odometer = entry.odometerKm,
                cost = entry.totalCostPhp,
                notes = entry.notes
            )
        )
    }

    // Add all part replacements
    partReplacements.forEach { entry ->
        events.add(
            HistoryEvent(
                date = entry.dateIso,
                description = entry.partName,
                odometer = entry.odometerKm,
                cost = entry.totalCostPhp,
                notes = entry.notes
            )
        )
    }

    // Sort by date in descending order (most recent first)
    val sortedEvents = events.sortedByDescending { it.date }

    val rows = sortedEvents.joinToString(separator = "") { event ->
        """
        <tr>
            <td>${event.date.htmlEscape()}</td>
            <td>${event.description.htmlEscape()}</td>
            <td>${event.odometer}</td>
            <td>PHP ${"%.2f".format(event.cost)}</td>
            <td>${event.notes.htmlEscape()}</td>
        </tr>
        """.trimIndent()
    }

    val displayName = motorcycleName.ifBlank { "Motorcycle" }.htmlEscape()

    return """
        <html>
        <head>
            <meta charset="utf-8" />
            <style>
                body { font-family: sans-serif; margin: 24px; }
                h1 { margin: 0 0 4px 0; font-size: 20px; }
                p { margin: 0 0 16px 0; color: #666; }
                table { border-collapse: collapse; width: 100%; }
                th, td { border: 1px solid #ddd; padding: 8px; vertical-align: top; }
                th { background: #f5f5f5; text-align: left; }
            </style>
        </head>
        <body>
            <h1>Maintenance & Parts Replacement History</h1>
            <p>$displayName</p>
            <table>
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Service/Part</th>
                        <th>Odometer (km)</th>
                        <th>Cost</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>
        </body>
        </html>
    """.trimIndent()
}

private fun String.htmlEscape(): String =
    this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")



