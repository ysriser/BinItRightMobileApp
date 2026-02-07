package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WasteCategoryMapperTest {

    @Test
    fun mapCategoryToWasteType_mapsTier2FreeTextToKnownType() {
        assertEquals(WasteCategoryMapper.TYPE_EWASTE, WasteCategoryMapper.mapCategoryToWasteType("E-waste - Phone Charger"))
        assertEquals(WasteCategoryMapper.TYPE_LIGHTING, WasteCategoryMapper.mapCategoryToWasteType("LED bulb"))
        assertEquals(WasteCategoryMapper.TYPE_METAL, WasteCategoryMapper.mapCategoryToWasteType("Metal can"))
        assertEquals(WasteCategoryMapper.TYPE_OTHERS, WasteCategoryMapper.mapCategoryToWasteType("Ceramic mug"))
    }

    @Test
    fun mapWasteTypeToBinType_returnsThreeSupportedBinTypes() {
        assertEquals("BLUEBIN", WasteCategoryMapper.mapWasteTypeToBinType(WasteCategoryMapper.TYPE_PLASTIC))
        assertEquals("EWASTE", WasteCategoryMapper.mapWasteTypeToBinType(WasteCategoryMapper.TYPE_EWASTE))
        assertEquals("LIGHTING", WasteCategoryMapper.mapWasteTypeToBinType(WasteCategoryMapper.TYPE_LIGHTING))
        assertEquals("", WasteCategoryMapper.mapWasteTypeToBinType(WasteCategoryMapper.TYPE_OTHERS))
    }

    @Test
    fun shouldDisplayAsRecyclable_honorsSpecialStreams() {
        assertTrue(WasteCategoryMapper.shouldDisplayAsRecyclable("E-waste - Charger", false))
        assertTrue(WasteCategoryMapper.shouldDisplayAsRecyclable("Lighting tube", false))
        assertTrue(WasteCategoryMapper.shouldDisplayAsRecyclable("Textile - T-shirt", false))
        assertFalse(WasteCategoryMapper.shouldDisplayAsRecyclable("Not sure", false))
        assertFalse(WasteCategoryMapper.shouldDisplayAsRecyclable("other_uncertain", false))
    }
}
