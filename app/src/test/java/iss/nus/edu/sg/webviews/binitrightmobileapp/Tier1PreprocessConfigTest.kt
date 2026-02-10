package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Tier1PreprocessConfigTest {

    @Test
    fun preprocessConfig_hasValidInputAndNormalization() {
        assertTrue(Tier1PreprocessConfig.INPUT_SIZE >= 224)
        assertTrue(Tier1PreprocessConfig.RESIZE_SCALE >= 1.0f)

        assertEquals(3, Tier1PreprocessConfig.NORMALIZE_MEAN.size)
        assertEquals(3, Tier1PreprocessConfig.NORMALIZE_STD.size)

        Tier1PreprocessConfig.NORMALIZE_STD.forEach { std ->
            assertTrue(std > 0f)
        }
    }

    @Test
    fun thresholds_keepStrictClassesAboveDefault() {
        assertTrue(Tier1PreprocessConfig.CONF_THRESHOLD_PLASTIC >= Tier1PreprocessConfig.CONF_THRESHOLD_DEFAULT)
        assertTrue(Tier1PreprocessConfig.CONF_THRESHOLD_GLASS >= Tier1PreprocessConfig.CONF_THRESHOLD_DEFAULT)
        assertTrue(Tier1PreprocessConfig.MARGIN_THRESHOLD >= 0f)
    }

    @Test
    fun modelAssetName_isOnnx() {
        assertTrue(Tier1PreprocessConfig.MODEL_ASSET_NAME.endsWith(".onnx"))
    }
}