package iss.nus.edu.sg.webviews.binitrightmobileapp

import iss.nus.edu.sg.webviews.binitrightmobileapp.model.Accessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.ChatRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.ChatResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.LeaderboardEntry
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RedeemResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterRequest
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.RegisterResponse
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserAccessory
import iss.nus.edu.sg.webviews.binitrightmobileapp.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDataContractsTest {

    @Test
    fun accessory_defaultsAreFalse() {
        val accessory = Accessory(
            accessoriesId = 10L,
            name = "Blue Cap",
            imageUrl = "https://img.example/blue_cap.png",
            requiredPoints = 100
        )

        assertFalse(accessory.owned)
        assertFalse(accessory.equipped)
    }

    @Test
    fun userAccessory_keepsNestedAccessoryFields() {
        val accessory = Accessory(1L, "Green Hat", "url", 50, owned = true)
        val userAccessory = UserAccessory(userAccessoriesId = 7L, equipped = true, accessories = accessory)

        assertTrue(userAccessory.equipped)
        assertEquals("Green Hat", userAccessory.accessories.name)
    }

    @Test
    fun userProfile_keepsAllValues() {
        val profile = UserProfile(
            name = "alex",
            pointBalance = 120,
            equippedAvatarName = "Blue Cap",
            totalRecycled = 11,
            aiSummary = "Great progress",
            totalAchievement = 4,
            carbonEmissionSaved = 2.8
        )

        assertEquals("alex", profile.name)
        assertEquals(120, profile.pointBalance)
        assertEquals(2.8, profile.carbonEmissionSaved, 0.0001)
    }

    @Test
    fun registerAndRedeemResponses_keepPayload() {
        val request = RegisterRequest("user1", "pass1")
        val registerResponse = RegisterResponse(success = true, message = "created")
        val redeemResponse = RedeemResponse(newTotalPoints = 888, message = "ok")

        assertEquals("user1", request.username)
        assertTrue(registerResponse.success)
        assertEquals(888, redeemResponse.newTotalPoints)
    }

    @Test
    fun leaderboardEntry_keepsRankingData() {
        val entry = LeaderboardEntry(userId = 88L, username = "sam", totalQuantity = 32)

        assertEquals(88L, entry.userId)
        assertEquals("sam", entry.username)
        assertEquals(32, entry.totalQuantity)
    }

    @Test
    fun chatRequestAndResponse_keepText() {
        val request = ChatRequest("How to recycle this cup?")
        val response = ChatResponse("Rinse and put in blue bin")

        assertEquals("How to recycle this cup?", request.message)
        assertEquals("Rinse and put in blue bin", response.reply)
    }

    @Test
    fun presignRequestAndResponse_keepFields() {
        val request = PresignUploadRequest(userId = 123L)
        val response = PresignUploadResponse(
            uploadUrl = "https://upload.example/signed",
            objectKey = "videos/user123/demo.mp4"
        )

        assertEquals(123L, request.userId)
        assertEquals("videos/user123/demo.mp4", response.objectKey)
    }

    @Test
    fun guidanceAndSerializableOutcome_keepInstructionalData() {
        val guidance = GuidanceResult(
            categoryTitle = "Ceramic Mug",
            disposalLabel = "General Waste",
            certainty = Certainty.MEDIUM,
            explanation = "Ceramic is not accepted in blue bins",
            tips = listOf("Wrap sharp edges")
        )
        val outcome = SerializableOutcome(
            categoryTitle = "Ceramic Mug",
            disposalLabel = "General Waste",
            certainty = "MEDIUM",
            explanation = "Not blue-bin recyclable",
            tips = listOf("Wrap before disposal"),
            instruction = "Dispose as general waste"
        )

        assertEquals(Certainty.MEDIUM, guidance.certainty)
        assertEquals("Dispose as general waste", outcome.instruction)
    }
}