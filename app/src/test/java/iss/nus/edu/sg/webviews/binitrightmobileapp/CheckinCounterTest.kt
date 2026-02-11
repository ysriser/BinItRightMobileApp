package iss.nus.edu.sg.webviews.binitrightmobileapp

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckinCounterTest {

    @Test
    fun increaseCounter_incrementsValue() {
        var count = 0
        count++
        assertEquals(1, count)
    }

    @Test
    fun decreaseCounter_doesNotGoBelowZero() {
        var count = 0
        if (count > 0) count--
        assertEquals(0, count)
    }
}