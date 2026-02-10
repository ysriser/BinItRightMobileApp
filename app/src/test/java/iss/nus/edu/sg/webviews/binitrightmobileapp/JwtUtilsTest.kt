package iss.nus.edu.sg.webviews.binitrightmobileapp

import java.nio.charset.StandardCharsets
import java.util.Base64
import iss.nus.edu.sg.webviews.binitrightmobileapp.utils.JwtUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class JwtUtilsTest {

    @Test
    fun getUserIdFromToken_returnsUserIdWhenTokenValid() {
        val token = buildToken("{" +
            "\"sub\":\"123\"," +
            "\"username\":\"alice\"," +
            "\"role\":\"USER\"" +
            "}")

        assertEquals(123L, JwtUtils.getUserIdFromToken(token))
    }

    @Test
    fun getUserIdFromToken_returnsNullWhenSubIsNotNumber() {
        val token = buildToken("{" +
            "\"sub\":\"abc\"," +
            "\"username\":\"alice\"" +
            "}")

        assertNull(JwtUtils.getUserIdFromToken(token))
    }

    @Test
    fun getUsernameFromToken_returnsNullWhenMissing() {
        val token = buildToken("{" +
            "\"sub\":\"123\"" +
            "}")

        assertNull(JwtUtils.getUsernameFromToken(token))
    }

    @Test
    fun getRoleFromToken_returnsRoleWhenPresent() {
        val token = buildToken("{" +
            "\"sub\":\"456\"," +
            "\"role\":\"ADMIN\"" +
            "}")

        assertEquals("ADMIN", JwtUtils.getRoleFromToken(token))
    }

    @Test
    fun jwtMethods_returnNullWhenTokenMalformed() {
        val malformed = "not-a-jwt"

        assertNull(JwtUtils.getUserIdFromToken(malformed))
        assertNull(JwtUtils.getUsernameFromToken(malformed))
        assertNull(JwtUtils.getRoleFromToken(malformed))
    }

    private fun buildToken(payloadJson: String): String {
        val header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}")
        val payload = base64Url(payloadJson)
        return "$header.$payload.signature"
    }

    private fun base64Url(input: String): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(input.toByteArray(StandardCharsets.UTF_8))
    }
}
