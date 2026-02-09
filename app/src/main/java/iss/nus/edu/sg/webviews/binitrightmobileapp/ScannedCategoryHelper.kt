package iss.nus.edu.sg.webviews.binitrightmobileapp

import java.util.Locale

object ScannedCategoryHelper {

    private val uncertainKeywords = listOf(
        "not sure",
        "uncertain",
        "unknown",
        "other_uncertain"
    )

    fun normalizeCategory(raw: String?): String {
        return raw?.trim()?.replace("\\s+".toRegex(), " ").orEmpty()
    }

    fun toDisplayCategory(raw: String?): String {
        val category = normalizeCategory(raw)
        if (category.isBlank()) {
            return "Unknown"
        }

        return when {
            category.startsWith("E-waste - ", ignoreCase = true) -> "E-waste"
            category.startsWith("Textile - ", ignoreCase = true) -> "Textile"
            category.startsWith("Lighting - ", ignoreCase = true) -> "Lighting"
            else -> category
        }
    }

    fun isUncertain(raw: String?): Boolean {
        val normalized = normalizeCategory(raw).lowercase(Locale.ROOT)
        return uncertainKeywords.any { normalized.contains(it) }
    }

    fun isSpecialRecyclable(raw: String?): Boolean {
        val normalized = normalizeCategory(raw).lowercase(Locale.ROOT)
        return normalized.startsWith("e-waste")
                || normalized.startsWith("textile")
                || normalized.startsWith("lighting")
                || normalized.contains("e-waste")
                || normalized.contains("electronic")
                || normalized.contains("battery")
                || normalized.contains("lamp")
                || normalized.contains("bulb")
    }

    fun toCheckInWasteType(raw: String?): String {
        val normalized = normalizeCategory(raw).lowercase(Locale.ROOT)
        return when {
            normalized.contains("plastic") -> "Plastic"
            normalized.contains("paper") || normalized.contains("cardboard") -> "Paper"
            normalized.contains("glass") || normalized.contains("ceramic") || normalized.contains("mug") -> "Glass"
            normalized.contains("metal") || normalized.contains("can") -> "Metal"
            normalized.contains("e-waste")
                    || normalized.contains("ewaste")
                    || normalized.contains("electronic")
                    || normalized.contains("battery")
                    || normalized.contains("charger")
                    || normalized.contains("cable") -> "E-Waste"
            normalized.contains("lighting")
                    || normalized.contains("lamp")
                    || normalized.contains("bulb")
                    || normalized.contains("tube light") -> "Lighting"
            normalized.contains("textile")
                    || normalized.contains("fabric")
                    || normalized.contains("clothes")
                    || normalized.contains("shoe") -> "Others"
            else -> "Others"
        }
    }

    fun toCheckInWasteTypeFromBinType(rawBinType: String?): String {
        return when (normalizeBinType(rawBinType)) {
            "EWASTE" -> "E-Waste"
            "LIGHTING" -> "Lighting"
            else -> "Others"
        }
    }

    fun normalizeBinType(rawBinType: String?): String {
        return when (normalizeCategory(rawBinType).uppercase(Locale.ROOT)) {
            "BLUEBIN" -> "BLUEBIN"
            "EWASTE" -> "EWASTE"
            "LIGHTING" -> "LIGHTING"
            else -> ""
        }
    }

    fun isLikelyRecyclableCategory(raw: String?): Boolean {
        val normalized = normalizeCategory(raw).lowercase(Locale.ROOT)
        return normalized.contains("plastic")
                || normalized.contains("paper")
                || normalized.contains("cardboard")
                || normalized.contains("glass")
                || normalized.contains("metal")
                || normalized.contains("can")
                || normalized.contains("e-waste")
                || normalized.contains("ewaste")
                || normalized.contains("electronic")
                || normalized.contains("battery")
                || normalized.contains("lighting")
                || normalized.contains("lamp")
                || normalized.contains("bulb")
                || normalized.contains("textile")
                || normalized.contains("fabric")
                || normalized.contains("clothes")
                || normalized.contains("shoe")
    }

    fun toBinType(rawCategory: String?, recyclable: Boolean): String {
        val normalized = normalizeCategory(rawCategory).lowercase(Locale.ROOT)
        return when {
            normalized.contains("lighting")
                    || normalized.contains("lamp")
                    || normalized.contains("bulb")
                    || normalized.contains("tube light") -> "LIGHTING"
            normalized.contains("e-waste")
                    || normalized.contains("ewaste")
                    || normalized.contains("electronic")
                    || normalized.contains("battery")
                    || normalized.contains("charger")
                    || normalized.contains("cable") -> "EWASTE"
            normalized.contains("textile")
                    || normalized.contains("fabric")
                    || normalized.contains("clothes")
                    || normalized.contains("shoe") -> ""
            recyclable
                    || normalized.contains("plastic")
                    || normalized.contains("paper")
                    || normalized.contains("glass")
                    || normalized.contains("metal")
                    || normalized.contains("cardboard") -> "BLUEBIN"
            else -> ""
        }
    }
}
