package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.FloatBuffer

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ImageClassifierTest {

    @Test
    fun classify_returnsError_whenModelIsUnavailable() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val classifier = ImageClassifier(context, inputSize = 8, resizeScale = 1.0f)
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

        val result = classifier.classify(bitmap)

        assertEquals("error", result.category)
        assertEquals(0f, result.confidence)
        assertTrue(result.top3.isEmpty())
        assertTrue(result.escalate)
    }

    @Test
    fun softmax_and_topK_work_via_reflection() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val classifier = ImageClassifier(context, inputSize = 4, resizeScale = 1.0f)

        val softmax = classifier.javaClass.getDeclaredMethod("softmax", FloatArray::class.java)
        softmax.isAccessible = true
        val probs = softmax.invoke(classifier, floatArrayOf(3f, 1f, 0f)) as FloatArray

        assertEquals(3, probs.size)
        assertTrue(probs[0] > probs[1] && probs[1] > probs[2])
        assertTrue(kotlin.math.abs(probs.sum() - 1f) < 1e-4f)

        val topK = classifier.javaClass.getDeclaredMethod("getTopKIndices", FloatArray::class.java, Int::class.javaPrimitiveType)
        topK.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val top = topK.invoke(classifier, floatArrayOf(0.3f, 0.9f, 0.5f), 2) as List<Int>
        assertEquals(listOf(1, 2), top)
    }

    @Test
    fun preprocess_and_resizeShorterSide_produce_expected_shapes() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val classifier = ImageClassifier(context, inputSize = 4, resizeScale = 1.0f)
        val source = Bitmap.createBitmap(2, 4, Bitmap.Config.ARGB_8888)
        source.eraseColor(0xFF336699.toInt())

        val resizeMethod = classifier.javaClass.getDeclaredMethod(
            "resizeShorterSide",
            Bitmap::class.java,
            Int::class.javaPrimitiveType
        )
        resizeMethod.isAccessible = true
        val resized = resizeMethod.invoke(classifier, source, 4) as Bitmap
        assertTrue(resized.width >= 4 && resized.height >= 4)

        val preprocessMethod = classifier.javaClass.getDeclaredMethod("preprocess", Bitmap::class.java)
        preprocessMethod.isAccessible = true
        val buffer = preprocessMethod.invoke(classifier, Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)) as FloatBuffer
        assertEquals(3 * 4 * 4, buffer.capacity())
    }

    @Test
    fun resizeShorterSide_sameSize_returnsOriginal_andTopKHandlesLargeK() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val classifier = ImageClassifier(context, inputSize = 4, resizeScale = 1.0f)
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)

        val resizeMethod = classifier.javaClass.getDeclaredMethod(
            "resizeShorterSide",
            Bitmap::class.java,
            Int::class.javaPrimitiveType
        )
        resizeMethod.isAccessible = true
        val resized = resizeMethod.invoke(classifier, source, 4) as Bitmap
        assertTrue(resized === source)

        val topK = classifier.javaClass.getDeclaredMethod(
            "getTopKIndices",
            FloatArray::class.java,
            Int::class.javaPrimitiveType
        )
        topK.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val result = topK.invoke(classifier, floatArrayOf(0.2f, 0.1f), 5) as List<Int>
        assertEquals(listOf(0, 1), result)
    }

    @Test
    fun preprocess_withSmallerInput_coversResizeRecycleBranch_andSoftmaxEmptyFallback() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val classifier = ImageClassifier(context, inputSize = 4, resizeScale = 1.25f)

        val preprocessMethod = classifier.javaClass.getDeclaredMethod("preprocess", Bitmap::class.java)
        preprocessMethod.isAccessible = true
        val buffer = preprocessMethod.invoke(
            classifier,
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        ) as FloatBuffer
        assertEquals(3 * 4 * 4, buffer.capacity())

        val softmax = classifier.javaClass.getDeclaredMethod("softmax", FloatArray::class.java)
        softmax.isAccessible = true
        val empty = softmax.invoke(classifier, floatArrayOf()) as FloatArray
        assertTrue(empty.isEmpty())
    }
}
