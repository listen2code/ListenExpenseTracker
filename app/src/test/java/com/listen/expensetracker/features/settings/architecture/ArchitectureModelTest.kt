package com.listen.expensetracker.features.settings.architecture

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.ArchitectureStrings
import com.listen.expensetracker.features.settings.viewmodel.SettingsDialog
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * 架构全景数据模型与多语言字典单元测试 (ArchitectureModelTest)。
 */
class ArchitectureModelTest {

    @Before
    fun setUp() {
        ArchitectureStrings.init()
    }

    @Test
    fun testMviNodesIntegrity() {
        val nodes = ArchitectureModelProvider.getMviNodes()
        assertEquals(5, nodes.size)

        val expectedIds = listOf("view", "intent", "viewmodel", "state", "effect")
        assertEquals(expectedIds, nodes.map { it.id })

        nodes.forEach { node ->
            assertTrue(node.titleKey.isNotBlank())
            assertTrue(node.subtitle.isNotBlank())
            assertTrue(node.descKey.isNotBlank())
            assertTrue(node.badge.isNotBlank())
            assertTrue(node.examples.isNotEmpty())
            assertTrue(node.rules.isNotEmpty())
            assertTrue(node.colorHex > 0L)

            // 校验多语言三态解析正常
            assertTrue(node.titleKey.tr("zh").isNotBlank())
            assertTrue(node.titleKey.tr("en").isNotBlank())
            assertTrue(node.titleKey.tr("ja").isNotBlank())
            assertTrue(node.descKey.tr("zh").isNotBlank())
        }
    }

    @Test
    fun testCleanLayersNodesIntegrity() {
        val nodes = ArchitectureModelProvider.getCleanLayersNodes()
        assertEquals(4, nodes.size)

        val expectedIds = listOf("presentation", "domain", "data", "core")
        assertEquals(expectedIds, nodes.map { it.id })

        nodes.forEach { node ->
            assertTrue(node.titleKey.isNotBlank())
            assertTrue(node.subtitle.isNotBlank())
            assertTrue(node.descKey.isNotBlank())
            assertTrue(node.badge.isNotBlank())
            assertTrue(node.examples.isNotEmpty())
            assertTrue(node.rules.isNotEmpty())

            assertTrue(node.titleKey.tr("zh").isNotBlank())
            assertTrue(node.titleKey.tr("en").isNotBlank())
            assertTrue(node.titleKey.tr("ja").isNotBlank())
        }
    }

    @Test
    fun testModuleNodesIntegrity() {
        val nodes = ArchitectureModelProvider.getModuleNodes()
        assertEquals(4, nodes.size)

        val expectedIds = listOf("mod_app", "mod_arch", "mod_ui", "mod_sys")
        assertEquals(expectedIds, nodes.map { it.id })

        nodes.forEach { node ->
            assertTrue(node.titleKey.isNotBlank())
            assertTrue(node.subtitle.isNotBlank())
            assertTrue(node.descKey.isNotBlank())
            assertTrue(node.badge.isNotBlank())
            assertTrue(node.examples.isNotEmpty())
            assertTrue(node.rules.isNotEmpty())

            assertTrue(node.titleKey.tr("zh").isNotBlank())
            assertTrue(node.titleKey.tr("en").isNotBlank())
            assertTrue(node.titleKey.tr("ja").isNotBlank())
        }
    }

    @Test
    fun testArchitectureStringsDictionary() {
        assertEquals("架构设计全景可视化", ArchitectureStrings.TITLE.tr("zh"))
        assertEquals("Architecture Visualizer", ArchitectureStrings.TITLE.tr("en"))
        assertEquals("アーキテクチャ全景ビジュアライザー", ArchitectureStrings.TITLE.tr("ja"))

        assertEquals("MVI 响应式流", ArchitectureStrings.TAB_MVI.tr("zh"))
        assertEquals("Clean Architecture", ArchitectureStrings.TAB_CLEAN.tr("zh"))
        assertEquals("模块解耦拓扑", ArchitectureStrings.TAB_MODULES.tr("zh"))
    }

    @Test
    fun testSettingsDialogArchitectureVisualizerType() {
        val dialog: SettingsDialog = SettingsDialog.ArchitectureVisualizer
        assertEquals(SettingsDialog.ArchitectureVisualizer, dialog)
    }
}
