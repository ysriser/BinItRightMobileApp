package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckinDataResponseTest {

        @Test
        fun response_success_isParsedCorrectly() {
            val response = CheckInDataResponse(
                responseCode = "SUCCESS",
                responseDesc = "Check-in completed"
            )

            assertEquals("SUCCESS", response.responseCode)
            assertEquals("Check-in completed", response.responseDesc)
        }


}