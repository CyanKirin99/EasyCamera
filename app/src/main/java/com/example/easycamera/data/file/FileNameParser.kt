package com.example.easycamera.data.file

data class ParsedPhotoInfo(
    val region: String,
    val date: String,
    val fieldCode: String,
    val sampleCode: String,
    val angleCode: String,
    val longitude: String,
    val latitude: String
)

object FileNameParser {

    private val VALID_EXTENSIONS = listOf(".jpg", ".jpeg")

    fun parse(filename: String): ParsedPhotoInfo? {
        val lower = filename.lowercase()
        val ext = VALID_EXTENSIONS.firstOrNull { lower.endsWith(it) } ?: return null
        val nameWithoutExt = filename.substring(0, filename.length - ext.length)

        val parts = nameWithoutExt.split("_")
        if (parts.size != 7) return null

        return ParsedPhotoInfo(
            region = parts[0],
            date = parts[1],
            fieldCode = parts[2],
            sampleCode = parts[3],
            angleCode = parts[4],
            longitude = parts[5],
            latitude = parts[6]
        )
    }
}