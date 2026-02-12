package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScannedCategoryHelperTest {

    @Test
    fun normalizeCategory_trimsAndCollapsesSpaces() {
        assertEquals("plastic bottle", ScannedCategoryHelper.normalizeCategory("  plastic   bottle  "))
        assertEquals("", ScannedCategoryHelper.normalizeCategory(null))
    }

    @Test
    fun toDisplayCategory_handlesSpecialPrefixes() {
        assertEquals("E-waste", ScannedCategoryHelper.toDisplayCategory("E-waste - Phone Charger"))
        assertEquals("Textile", ScannedCategoryHelper.toDisplayCategory("Textile - Old Shirt"))
        assertEquals("Lighting", ScannedCategoryHelper.toDisplayCategory("Lighting - LED Bulb"))
        assertEquals("Glass", ScannedCategoryHelper.toDisplayCategory("Glass - Bottle"))
        assertEquals("Metal", ScannedCategoryHelper.toDisplayCategory("Metal - Soda Can"))
        assertEquals("Plastic", ScannedCategoryHelper.toDisplayCategory("Plastic - Container"))
        assertEquals("Paper", ScannedCategoryHelper.toDisplayCategory("Paper - Cardboard"))
    }

    @Test
    fun toDisplayCategory_returnsUnknownForBlankInput() {
        assertEquals("Unknown", ScannedCategoryHelper.toDisplayCategory("   "))
        assertEquals("Unknown", ScannedCategoryHelper.toDisplayCategory(null))
    }

    @Test
    fun isUncertain_detectsKnownKeywords() {
        assertTrue(ScannedCategoryHelper.isUncertain("Not sure what item this is"))
        assertTrue(ScannedCategoryHelper.isUncertain("other_uncertain"))
        assertFalse(ScannedCategoryHelper.isUncertain("plastic bottle"))
    }

    @Test
    fun isSpecialRecyclable_detectsSpecialStreams() {
        assertTrue(ScannedCategoryHelper.isSpecialRecyclable("E-waste - Charger"))
        assertTrue(ScannedCategoryHelper.isSpecialRecyclable("Textile - Old Shirt"))
        assertTrue(ScannedCategoryHelper.isSpecialRecyclable("Lighting - LED Bulb"))
        assertFalse(ScannedCategoryHelper.isSpecialRecyclable("Paper cup"))
    }

    @Test
    fun toCheckInWasteType_mapsTier2LikeCategories() {
        assertEquals("Others", ScannedCategoryHelper.toCheckInWasteType("ceramic mug"))
        assertEquals("E-Waste", ScannedCategoryHelper.toCheckInWasteType("usb charger cable"))
        assertEquals("Others", ScannedCategoryHelper.toCheckInWasteType("old textile shirt"))
        assertEquals("Paper", ScannedCategoryHelper.toCheckInWasteType("cardboard box"))
    }

    @Test
    fun toBinType_mapsCategoryAndRecyclableToExpectedBin() {
        assertEquals("", ScannedCategoryHelper.toBinType("LED lamp", recyclable = false))
        assertEquals("", ScannedCategoryHelper.toBinType("E-waste - Charger", recyclable = false))
        assertEquals("LIGHTING", ScannedCategoryHelper.toBinType("LED lamp", recyclable = true))
        assertEquals("EWASTE", ScannedCategoryHelper.toBinType("E-waste - Charger", recyclable = true))
        assertEquals("", ScannedCategoryHelper.toBinType("Textile - Shirt", recyclable = true))
        assertEquals("BLUEBIN", ScannedCategoryHelper.toBinType("plastic bottle", recyclable = true))
    }

    @Test
    fun normalizeBinType_acceptsOnlySupportedValues() {
        assertEquals("BLUEBIN", ScannedCategoryHelper.normalizeBinType("bluebin"))
        assertEquals("EWASTE", ScannedCategoryHelper.normalizeBinType("EWASTE"))
        assertEquals("EWASTE", ScannedCategoryHelper.normalizeBinType("E-Waste Bin"))
        assertEquals("EWASTE", ScannedCategoryHelper.normalizeBinType("electronic"))
        assertEquals("LIGHTING", ScannedCategoryHelper.normalizeBinType("lighting"))
        assertEquals("LIGHTING", ScannedCategoryHelper.normalizeBinType("LED bulb"))
        assertEquals("", ScannedCategoryHelper.normalizeBinType("TEXTILE"))
    }

    @Test
    fun isLikelyRecyclableCategory_handlesSpecialAndNormalCases() {
        assertTrue(ScannedCategoryHelper.isLikelyRecyclableCategory("metal can"))
        assertTrue(ScannedCategoryHelper.isLikelyRecyclableCategory("battery pack"))
        assertFalse(ScannedCategoryHelper.isLikelyRecyclableCategory("random toy"))
    }

    @Test
    fun toCheckInWasteTypeFromBinType_mapsOnlySupportedBins() {
        assertEquals("E-Waste", ScannedCategoryHelper.toCheckInWasteTypeFromBinType("EWASTE"))
        assertEquals("Lighting", ScannedCategoryHelper.toCheckInWasteTypeFromBinType("LIGHTING"))
        assertEquals("", ScannedCategoryHelper.toCheckInWasteTypeFromBinType("BLUEBIN"))
    }

    @Test
    fun isCategoryRecyclable_requiresKnownWasteCategoriesOnly() {
        assertTrue(ScannedCategoryHelper.isCategoryRecyclable("Glass bottle"))
        assertFalse(ScannedCategoryHelper.isCategoryRecyclable("ceramic mug"))
        assertFalse(ScannedCategoryHelper.isCategoryRecyclable("other_uncertain"))
    }
}
