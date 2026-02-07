package iss.nus.edu.sg.webviews.binitrightmobileapp

object WasteCategoryMapper {
    const val TYPE_PLASTIC = "Plastic"
    const val TYPE_PAPER = "Paper"
    const val TYPE_GLASS = "Glass"
    const val TYPE_METAL = "Metal"
    const val TYPE_EWASTE = "E-Waste"
    const val TYPE_LIGHTING = "Lighting"
    const val TYPE_OTHERS = "Others"

    fun mapCategoryToWasteType(category: String?): String {
        if (category.isNullOrBlank()) {
            return TYPE_OTHERS
        }

        val text = category.trim().lowercase()

        if (text.contains("e-waste") || text.contains("ewaste") || text.contains("electronic")
            || text.contains("battery") || text.contains("charger") || text.contains("cable")
            || text.contains("adapter")
        ) {
            return TYPE_EWASTE
        }

        if (text.contains("lighting") || text.contains("lamp") || text.contains("bulb")
            || text.contains("led") || text.contains("fluorescent")
        ) {
            return TYPE_LIGHTING
        }

        if (text.contains("plastic") || text.contains("pet") || text.contains("pp")
            || text.contains("hdpe") || text.contains("wrapper") || text.contains("styrofoam")
        ) {
            return TYPE_PLASTIC
        }

        if (text.contains("paper") || text.contains("cardboard") || text.contains("carton")
            || text.contains("newspaper") || text.contains("book")
        ) {
            return TYPE_PAPER
        }

        if (text.contains("glass") || text.contains("jar")) {
            return TYPE_GLASS
        }

        if (text.contains("metal") || text.contains("aluminium") || text.contains("aluminum")
            || text.contains("steel") || text.contains("tin") || text.contains("can")
        ) {
            return TYPE_METAL
        }

        // Textile has no dedicated bin in current app flow, so route to Others for manual choice.
        if (text.contains("textile") || text.contains("fabric") || text.contains("clothes")
            || text.contains("shirt") || text.contains("shoe")
        ) {
            return TYPE_OTHERS
        }

        return TYPE_OTHERS
    }

    fun mapWasteTypeToBinType(wasteType: String?): String {
        return when (wasteType?.trim()?.lowercase()) {
            TYPE_EWASTE.lowercase() -> "EWASTE"
            TYPE_LIGHTING.lowercase() -> "LIGHTING"
            TYPE_PLASTIC.lowercase(),
            TYPE_PAPER.lowercase(),
            TYPE_GLASS.lowercase(),
            TYPE_METAL.lowercase() -> "BLUEBIN"
            else -> ""
        }
    }

    fun shouldDisplayAsRecyclable(category: String?, recyclableFromServer: Boolean): Boolean {
        if (recyclableFromServer) {
            return true
        }

        val normalized = category?.trim()?.lowercase() ?: return false
        if (normalized.contains("not sure") || normalized.contains("uncertain")
            || normalized.contains("unknown") || normalized.contains("other_uncertain")
        ) {
            return false
        }

        val mapped = mapCategoryToWasteType(category)
        return mapped == TYPE_EWASTE || mapped == TYPE_LIGHTING || normalized.contains("textile")
    }
}
