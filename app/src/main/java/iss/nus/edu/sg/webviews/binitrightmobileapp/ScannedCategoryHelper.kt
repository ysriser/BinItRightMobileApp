
    private fun mappingCategory(fullCategory: String): String {
        // If it starts with "E-waste - " or "Textile - ", strip the prefix to get the generic name if needed?
        // Actually the previous code used the full text. But the prompt says "category starts with 'E-waste - ' -> ewaste".
        // The previous code passed 'scannedCategory' (the text view content) as 'wasteCategory' bundle arg.
        // I will keep it as is, or maybe strip if users prefer.
        // For now, let's just return the full category.
        return fullCategory
    }
