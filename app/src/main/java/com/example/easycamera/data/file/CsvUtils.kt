package com.example.easycamera.data.file

import java.io.OutputStreamWriter
import java.io.File
import java.nio.charset.Charset

object CsvUtils {

    private val BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
    private const val CHARSET = "UTF-8"

    fun escapeField(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    fun formatLine(values: List<String>): String {
        return values.joinToString(",") { escapeField(it) } + "\n"
    }

    fun writeHeader(file: File, headers: List<String>): Boolean {
        return try {
            file.parentFile?.mkdirs()
            file.outputStream().use { out ->
                out.write(BOM)
                val writer = OutputStreamWriter(out, Charset.forName(CHARSET))
                writer.write(formatLine(headers))
                writer.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun appendLine(file: File, values: List<String>): Boolean {
        return try {
            if (!file.exists()) {
                return false
            }
            file.appendBytes(formatLine(values).toByteArray(Charset.forName(CHARSET)))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readAllLines(file: File): List<List<String>> {
        return try {
            if (!file.exists()) return emptyList()
            file.readText(Charset.forName(CHARSET))
                .trimStart('\uFEFF')
                .lineSequence()
                .filter { it.isNotBlank() }
                .map { line -> parseLine(line) }
                .toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes -> {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i += 2
                    } else {
                        inQuotes = false
                        i++
                    }
                }
                c == '"' && !inQuotes -> {
                    inQuotes = true
                    i++
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(c)
                    i++
                }
            }
        }
        result.add(current.toString())
        return result
    }
}