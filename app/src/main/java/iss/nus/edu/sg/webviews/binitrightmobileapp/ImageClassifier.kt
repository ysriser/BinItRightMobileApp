package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.util.*

class ImageClassifier(private val context: Context) {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    // Hardcoded labels as requested by user
    private val labels = listOf(
        "paper",
        "plastic",
        "metal",
        "glass",
        "other_uncertain",
        "e-waste",
        "textile"
    )

    init {
        initialize()
    }

    private fun initialize() {
        try {
            ortEnvironment = OrtEnvironment.getEnvironment()
            // Load model from assets
            val modelBytes = context.assets.open("tier1.onnx").readBytes()
            ortSession = ortEnvironment?.createSession(modelBytes)
            Log.d("ImageClassifier", "Model loaded successfully")
        } catch (e: Exception) {
            Log.e("ImageClassifier", "Error initializing model", e)
        }
    }

    fun classify(bitmap: Bitmap): String {
        if (ortSession == null) return "Error: Model not initialized"

        try {
            // 1. Preprocess
            val floatBuffer = preprocess(bitmap)

            // 2. Create Input Tensor
            // Shape: [1, 3, 224, 224]
            val inputName = ortSession?.inputNames?.iterator()?.next() ?: "input"
            val shape = longArrayOf(1, 3, 224, 224)
            val inputTensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)

            // 3. Run Inference
            val results = ortSession?.run(mapOf(inputName to inputTensor))
            
            // 4. Postprocess
            // output shape is typically [1, num_classes]
            // We assume the first output is the one we want
            val outputTensor = results?.get(0)?.value as Array<FloatArray>
            val logits = outputTensor[0]
            
            val predictedIndex = getMaxIndex(logits)
            
            inputTensor.close() // Close tensor to free memory
            
            // Ensure index is within bounds
            if (predictedIndex >= 0 && predictedIndex < labels.size) {
                 return labels[predictedIndex]
            }
            return "other_uncertain"

        } catch (e: Exception) {
            Log.e("ImageClassifier", "Error during classification", e)
            return "Error: ${e.message}"
        }
    }

    private fun preprocess(bitmap: Bitmap): FloatBuffer {
        // Resize to 257x257 (as per docs: int(224*1.15)=257)
        val resizeScale = 257
        val resized = Bitmap.createScaledBitmap(bitmap, resizeScale, resizeScale, true)

        // Center Crop to 224x224
        val targetSize = 224
        val xOffset = (resizeScale - targetSize) / 2
        val yOffset = (resizeScale - targetSize) / 2
        val cropped = Bitmap.createBitmap(resized, xOffset, yOffset, targetSize, targetSize)

        // Normalize & Convert to NCHW
        // mean: [0.485, 0.456, 0.406]
        // std: [0.229, 0.224, 0.225]
        val totalPixels = targetSize * targetSize
        val buffer = FloatBuffer.allocate(3 * totalPixels)
        
        // We will fill the buffer channel by channel: R first, then G, then B
        // Because ONNX expects NCHW, but Android Bitmap is usually packed pixels (RGBRGB...)
        // We need to iterate pixels and put them into separate regions of the buffer
        
        // However, standard intuitive loop for simpler reading:
        // Create 3 separate arrays or just write to specific offsets in one buffer
        
        // Let's use the user's provided snippet logic style but adapted for FloatBuffer
        for (y in 0 until targetSize) {
            for (x in 0 until targetSize) {
                val pixel = cropped.getPixel(x, y)
                
                // Extract RGB (0-255)
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f

                // Normalize
                val rNorm = (r - 0.485f) / 0.229f
                val gNorm = (g - 0.456f) / 0.224f
                val bNorm = (b - 0.406f) / 0.225f
                
                // Calculate NCHW indices
                // Index in the flattened [3, 224, 224] array
                // channel 0 (R): y * width + x
                // channel 1 (G): width*height + y * width + x
                // channel 2 (B): 2*width*height + y * width + x
                
                val pixelIndex = y * targetSize + x
                
                buffer.put(pixelIndex, rNorm)
                buffer.put(totalPixels + pixelIndex, gNorm)
                buffer.put(2 * totalPixels + pixelIndex, bNorm)
            }
        }
        
        return buffer
    }

    private fun getMaxIndex(array: FloatArray): Int {
        var maxIndex = -1
        var maxVal = Float.NEGATIVE_INFINITY
        for (i in array.indices) {
            if (array[i] > maxVal) {
                maxVal = array[i]
                maxIndex = i
            }
        }
        return maxIndex
    }
}
