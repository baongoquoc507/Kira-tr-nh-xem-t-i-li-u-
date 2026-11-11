package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleAppTest {
    @Test
    fun testAppNameConstant() {
        val appName = "Trình xem tài liệu Kira"
        assertTrue(appName.isNotEmpty())
        assertEquals("Trình xem tài liệu Kira", appName)
    }
}
