package iss.nus.edu.sg.webviews.binitrightmobileapp

import android.util.Base64
import iss.nus.edu.sg.webviews.binitrightmobileapp.utils.JwtUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JwtUtilsTest {

    @Test
    fun getUserIdFromToken_returnsSubAsLong() {
        val header = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}")
        val payload = b64("{\"sub\":\"123\",\"role\":\"USER\"}")
        val token = "$header.$payload.signature"

        assertEquals(123L, JwtUtils.getUserIdFromToken(token))
    }

    @Test
    fun getRoleFromToken_returnsRole() {
        val header = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}")
        val payload = b64("{\"sub\":\"123\",\"role\":\"ADMIN\"}")
        val token = "$header.$payload.signature"

        assertEquals("ADMIN", JwtUtils.getRoleFromToken(token))
    }

    @Test
    fun invalidToken_returnsNulls() {
        assertNull(JwtUtils.getUserIdFromToken("bad-token"))
        assertNull(JwtUtils.getRoleFromToken("bad-token"))
    }

    private fun b64(raw: String): String {
        return Base64.encodeToString(raw.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
    }
}