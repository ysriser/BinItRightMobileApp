package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ImageClassifierTest {

    @Test
    fun softmax_outputsSumToOne() {
        val classifier = createClassifier()

        val method = ImageClassifier::class.java
            .getDeclaredMethod("softmax", FloatArray::class.java)

        method.isAccessible = true

        val logits = floatArrayOf(1f, 2f, 3f)

        val result = method.invoke(classifier, logits) as FloatArray

        val sum = result.sum()

        assertEquals(1f, sum, 0.0001f)
    }

    @Test
    fun getTopKIndices_returnsCorrectOrder() {
        val classifier = createClassifier()

        val method = ImageClassifier::class.java
            .getDeclaredMethod("getTopKIndices", FloatArray::class.java, Int::class.javaPrimitiveType)

        method.isAccessible = true

        val array = floatArrayOf(0.1f, 0.9f, 0.5f, 0.2f)

        val result = method.invoke(classifier, array, 2) as List<*>

        assertEquals(listOf(1, 2), result)
    }

    @Test
    fun classify_whenSessionNull_returnsErrorResult() {
        val classifier = createClassifier()

        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        val result = classifier.classify(bitmap)

        assertEquals("error", result.category)
        assertTrue(result.escalate)
    }


    @Test
    fun resizeShorterSide_scalesCorrectly() {
        val classifier = createClassifier()

        val method = ImageClassifier::class.java
            .getDeclaredMethod("resizeShorterSide", Bitmap::class.java, Int::class.javaPrimitiveType)

        method.isAccessible = true

        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)

        val resized = method.invoke(classifier, bitmap, 128) as Bitmap

        assertTrue(resized.width >= 64)
        assertTrue(resized.height >= 64)
    }





    private fun createClassifier(): ImageClassifier {
        val context = RuntimeEnvironment.getApplication()
        return ImageClassifier(context)
    }
}