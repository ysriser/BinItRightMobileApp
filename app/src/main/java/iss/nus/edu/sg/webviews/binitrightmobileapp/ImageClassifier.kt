package iss.nus.edu.sg.webviews.binitrightmobileapp

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.get
import androidx.core.graphics.scale
import java.nio.FloatBuffer
import kotlin.math.roundToInt

class ImageClassifier(
    private val context: Context,
    private val inputSize: Int = Tier1PreprocessConfig.INPUT_SIZE,
    private val resizeScale: Float = Tier1PreprocessConfig.RESIZE_SCALE,
) {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

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
            val modelBytes = context.assets.open(Tier1PreprocessConfig.MODEL_ASSET_NAME).readBytes()
            ortSession = ortEnvironment?.createSession(modelBytes)
            Log.d("ImageClassifier", "Model loaded successfully")
        } catch (error: Throwable) {
            Log.e("ImageClassifier", "Error initializing model", error)
        }
    }

    fun classify(bitmap: Bitmap): Tier1Result {
        val session = ortSession ?: run {
            return Tier1Result("error", 0f, emptyList(), true)
        }

        return try {
            val floatBuffer = preprocess(bitmap)
            val inputName = session.inputNames.iterator().next()
            val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
            val inputTensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)

            val results = session.run(mapOf(inputName to inputTensor))
            @Suppress("UNCHECKED_CAST")
            val outputTensor = results[0].value as Array<FloatArray>
            val logits = outputTensor[0]

            results.close()
            inputTensor.close()

            val probabilities = softmax(logits)
            val top3Indices = getTopKIndices(probabilities, 3)
            val top3 = top3Indices.map { index ->
                mapOf("label" to labels[index], "p" to probabilities[index])
            }

            val top1Index = top3Indices[0]
            val top1Label = labels[top1Index]
            val top1Confidence = probabilities[top1Index]

            val confThreshold = when (top1Label.lowercase()) {
                "plastic" -> Tier1PreprocessConfig.CONF_THRESHOLD_PLASTIC
                "glass" -> Tier1PreprocessConfig.CONF_THRESHOLD_GLASS
                else -> Tier1PreprocessConfig.CONF_THRESHOLD_DEFAULT
            }

            val margin = if (probabilities.size > 1) {
                probabilities[top3Indices[0]] - probabilities[top3Indices[1]]
            } else {
                1.0f
            }

            val escalate = top1Label == "other_uncertain"
                    || top1Confidence < confThreshold
                    || margin < Tier1PreprocessConfig.MARGIN_THRESHOLD

            Tier1Result(
                category = top1Label,
                confidence = top1Confidence,
                top3 = top3,
                escalate = escalate
            )
        } catch (error: Exception) {
            Log.e("ImageClassifier", "Error during classification", error)
            Tier1Result("error", 0f, emptyList(), true)
        }
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val exp = logits.map { kotlin.math.exp(it - maxLogit) }
        val sumExp = exp.sum()
        return exp.map { it / sumExp }.toFloatArray()
    }

    private fun getTopKIndices(array: FloatArray, k: Int): List<Int> {
        return array.withIndex()
            .sortedByDescending { it.value }
            .take(k)
            .map { it.index }
    }

    private fun preprocess(bitmap: Bitmap): FloatBuffer {
        val targetShortSide = (inputSize.toFloat() * resizeScale).roundToInt().coerceAtLeast(inputSize)
        val resized = resizeShorterSide(bitmap, targetShortSide)

        val xOffset = ((resized.width - inputSize) / 2).coerceAtLeast(0)
        val yOffset = ((resized.height - inputSize) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(resized, xOffset, yOffset, inputSize, inputSize)

        if (resized !== bitmap) {
            resized.recycle()
        }

        val totalPixels = inputSize * inputSize
        val buffer = FloatBuffer.allocate(3 * totalPixels)

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = cropped[x, y]

                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f

                val rNorm = (r - Tier1PreprocessConfig.NORMALIZE_MEAN[0]) / Tier1PreprocessConfig.NORMALIZE_STD[0]
                val gNorm = (g - Tier1PreprocessConfig.NORMALIZE_MEAN[1]) / Tier1PreprocessConfig.NORMALIZE_STD[1]
                val bNorm = (b - Tier1PreprocessConfig.NORMALIZE_MEAN[2]) / Tier1PreprocessConfig.NORMALIZE_STD[2]

                val pixelIndex = y * inputSize + x
                buffer.put(pixelIndex, rNorm)
                buffer.put(totalPixels + pixelIndex, gNorm)
                buffer.put(2 * totalPixels + pixelIndex, bNorm)
            }
        }

        cropped.recycle()
        return buffer
    }

    private fun resizeShorterSide(source: Bitmap, targetShortSide: Int): Bitmap {
        val width = source.width
        val height = source.height
        val shortSide = minOf(width, height).coerceAtLeast(1)
        val scale = targetShortSide.toFloat() / shortSide.toFloat()

        val newWidth = (width * scale).roundToInt().coerceAtLeast(inputSize)
        val newHeight = (height * scale).roundToInt().coerceAtLeast(inputSize)

        if (newWidth == width && newHeight == height) {
            return source
        }
        return source.scale(newWidth, newHeight, true)
    }
}
