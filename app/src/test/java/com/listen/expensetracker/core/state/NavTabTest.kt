package com.listen.expensetracker.core.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NavTabTest {

    @Test
    fun testNavTabEnumValues() {
        val tabs = NavTab.values()
        assertEquals(3, tabs.size)

        assertEquals("transactions", NavTab.TRANSACTIONS.route)
        assertEquals("nav_transactions", NavTab.TRANSACTIONS.labelKey)
        assertNotNull(NavTab.TRANSACTIONS.icon)

        assertEquals("statistics", NavTab.STATISTICS.route)
        assertEquals("nav_statistics", NavTab.STATISTICS.labelKey)

        assertEquals("settings", NavTab.SETTINGS.route)
        assertEquals("nav_settings", NavTab.SETTINGS.labelKey)
    }

    @Test
    fun testAppOverlaySealedInterface() {
        val overlay: AppOverlay = AppOverlay.ApmInspector
        assertNotNull(overlay)
        assertEquals(AppOverlay.ApmInspector, overlay)
    }
}
