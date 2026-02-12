package iss.nus.edu.sg.webviews.binitrightmobileapp

object Tier1PreprocessConfig {
    // Change this value to 256 if you export a 256x256 model input.
    const val INPUT_SIZE = 256

    // Keep consistent with model export preprocessing.
    const val RESIZE_SCALE = 1.1f

    // Tier-1 escalation thresholds.
    const val CONF_THRESHOLD_DEFAULT = 0.65f
    const val CONF_THRESHOLD_PLASTIC = 0.70f
    const val CONF_THRESHOLD_GLASS = 0.70f
    const val MARGIN_THRESHOLD = 0.3f

    const val MODEL_ASSET_NAME = "tier1.onnx"

    val NORMALIZE_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    val NORMALIZE_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
}
