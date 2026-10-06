package com.bysinbin.posea.ui.main

import com.bysinbin.posea.model.IconStyle
import com.bysinbin.posea.model.LauncherMode
import com.bysinbin.posea.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherModelsTest {

    @Test
    fun defaultPreferences_isCorrect() {
        val prefs = UserPreferences()
        assertEquals(LauncherMode.HYBRID, prefs.mode)
        assertEquals(IconStyle.COLOR, prefs.iconStyle)
        assertFalse(prefs.isOnboardingCompleted)
        assertTrue(prefs.favoritePackages.isEmpty())
    }

    @Test
    fun launcherMode_hasValidDescriptions() {
        LauncherMode.entries.forEach { mode ->
            assertTrue(mode.title.isNotBlank())
            assertTrue(mode.description.isNotBlank())
        }
    }
}
