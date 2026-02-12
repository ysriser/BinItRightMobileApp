package iss.nus.edu.sg.webviews.binitrightmobileapp

import java.util.Locale

object ScannedCategoryHelper {
    private const val E_WASTE_KEYWORD = "e-waste"
    private const val WASTE_TYPE_PLASTIC = "Plastic"
    private const val WASTE_TYPE_PAPER = "Paper"
    private const val WASTE_TYPE_GLASS = "Glass"
    private const val WASTE_TYPE_METAL = "Metal"
    private const val WASTE_TYPE_EWASTE = "E-Waste"
    private const val WASTE_TYPE_LIGHTING = "Lighting"
    private const val WASTE_TYPE_OTHERS = "Others"

    private val recyclableWasteTypes = setOf(
        WASTE_TYPE_PLASTIC,
        WASTE_TYPE_PAPER,
        WASTE_TYPE_GLASS,
        WASTE_TYPE_METAL,
        WASTE_TYPE_EWASTE,
        WASTE_TYPE_LIGHTING,
    )

    private val uncertainKeywords = listOf(
        "not sure",
        "uncertain",
        "unknown",
        "other_uncertain"
    )

    private const val DISPLAY_TEXTILE = "Textile"
    private val tier2CategoryPrefixes = listOf(
        "plastic" to WASTE_TYPE_PLASTIC,
        "paper" to WASTE_TYPE_PAPER,
        "glass" to WASTE_TYPE_GLASS,
        "metal" to WASTE_TYPE_METAL,
        "lighting" to WASTE_TYPE_LIGHTING,
        "textile" to DISPLAY_TEXTILE,
        "e-waste" to "E-waste",
        "ewaste" to "E-waste",
        "e waste" to "E-waste",
    )

    fun normalizeCategory(raw: String?): String {
        return raw?.trim()?.replace("\\s+".toRegex(), " ").orEmpty()
    }

    fun toDisplayCategory(raw: String?): String {
        val category = normalizeCategory(raw)
        if (category.isBlank()) {
            return "Unknown"
        }

        return canonicalTier2Category(category) ?: category
    }

    fun isUncertain(raw: String?): Boolean {
        val normalized = normalizeCategory(raw).lowercase(Locale.ROOT)
        return uncertainKeywords.any { normalized.contains(it) }
    }

    fun isSpecialRecyclable(raw: String?): Boolean {
        return when (toCheckInWasteType(raw)) {
            WASTE_TYPE_EWASTE,
            WASTE_TYPE_LIGHTING -> true
            WASTE_TYPE_OTHERS -> isTextileCategory(raw)
            else -> false
        }
    }

    fun toCheckInWasteType(raw: String?): String {
        val normalized = normalizeCategory(raw).lowercase(Locale.ROOT)
        if (normalized.isBlank() || isUncertain(normalized)) {
            return WASTE_TYPE_OTHERS
        }

        return when {
            normalized.contains("plastic") -> WASTE_TYPE_PLASTIC
            normalized.contains("paper") || normalized.contains("cardboard") -> WASTE_TYPE_PAPER
            normalized.contains("glass") -> WASTE_TYPE_GLASS
            normalized.contains("metal") || normalized.contains("can") -> WASTE_TYPE_METAL
            normalized.contains(E_WASTE_KEYWORD)
                    || normalized.contains("ewaste")
                    || normalized.contains("electronic")
                    || normalized.contains("battery")
                    || normalized.contains("charger")
                    || normalized.contains("cable") -> WASTE_TYPE_EWASTE
            normalized.contains("lighting")
                    || normalized.contains("lamp")
                    || normalized.contains("bulb")
                    || normalized.contains("tube light") -> WASTE_TYPE_LIGHTING
            else -> WASTE_TYPE_OTHERS
        }
    }

    fun toCheckInWasteTypeFromBinType(rawBinType: String?): String {
        return when (normalizeBinType(rawBinType)) {
            "EWASTE" -> WASTE_TYPE_EWASTE
            "LIGHTING" -> WASTE_TYPE_LIGHTING
            else -> ""
        }
    }

    fun normalizeBinType(rawBinType: String?): String {
        val compact = normalizeCategory(rawBinType)
            .uppercase(Locale.ROOT)
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "")

        return when {
            compact == "BLUEBIN" || compact == "BLUE" || compact.contains("BLUEBIN") -> "BLUEBIN"
            compact == "EWASTE"
                    || compact.contains("EWASTE")
                    || compact.contains("EWASTEBIN")
                    || compact.contains("ELECTRONIC")
                    || compact.contains("BATTERY") -> "EWASTE"
            compact == "LIGHTING"
                    || compact.contains("LIGHTING")
                    || compact.contains("LAMP")
                    || compact.contains("BULB") -> "LIGHTING"
            else -> ""
        }
    }

    fun isLikelyRecyclableCategory(raw: String?): Boolean {
        return isCategoryRecyclable(raw)
    }

    fun isCategoryRecyclable(raw: String?): Boolean {
        if (isUncertain(raw)) {
            return false
        }
        if (isTextileCategory(raw)) {
            return true
        }
        return recyclableWasteTypes.contains(toCheckInWasteType(raw))
    }

    fun isTextileCategory(raw: String?): Boolean {
        val normalized = normalizeCategory(raw).lowercase(Locale.ROOT)
        return normalized.contains("textile")
                || normalized.contains("fabric")
                || normalized.contains("clothes")
                || normalized.contains("shoe")
    }

    fun toBinType(rawCategory: String?, recyclable: Boolean): String {
        if (!recyclable) {
            return ""
        }
        if (isTextileCategory(rawCategory)) {
            return ""
        }

        return when (toCheckInWasteType(rawCategory)) {
            WASTE_TYPE_EWASTE -> "EWASTE"
            WASTE_TYPE_LIGHTING -> "LIGHTING"
            WASTE_TYPE_PLASTIC,
            WASTE_TYPE_PAPER,
            WASTE_TYPE_GLASS,
            WASTE_TYPE_METAL -> "BLUEBIN"
            else -> ""
        }
    }

    private fun canonicalTier2Category(category: String): String? {
        val lower = category.lowercase(Locale.ROOT)
        return tier2CategoryPrefixes.firstNotNullOfOrNull { (prefix, canonical) ->
            if (matchesTier2Prefix(lower, prefix)) canonical else null
        }
    }

    private fun matchesTier2Prefix(lowerCategory: String, prefix: String): Boolean {
        return lowerCategory == prefix ||
                lowerCategory.startsWith("$prefix -") ||
                lowerCategory.startsWith("$prefix:") ||
                lowerCategory.startsWith("$prefix |")
    }
}
