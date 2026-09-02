package com.warungerik.devidchanger

import com.warungerik.devidchanger.model.GameVersion
import org.junit.Assert.assertEquals
import org.junit.Test

class GameVersionTest {
    @Test
    fun testIndiaPackageName() {
        assertEquals("com.mobiin.gp", GameVersion.INDIA.packageName)
    }

    @Test
    fun testAllVersionsDecodeNonEmpty() {
        GameVersion.entries.forEach { version ->
            assert(version.packageName.isNotEmpty()) { "Package name for ${version.displayName} should not be empty" }
        }
    }
}
