/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit

import kr.toxicity.model.api.version.MinecraftVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BukkitServerCompatibilityTest {
    @Test
    fun `selects only implemented adapters`() {
        val expected = mapOf(
            "1.21.1" to false,
            "1.21.3" to false,
            "1.21.4" to true,
            "1.21.8" to true,
            "1.21.11" to true,
            "26.2" to true
        )
        expected.forEach { (version, supported) ->
            val result = BukkitServerCompatibility.detect(MinecraftVersion.parse(version))
            assertEquals(supported, result is BukkitServerCompatibility.Supported, version)
        }
    }

    @Test
    fun `selects the production adapter`() {
        val result = BukkitServerCompatibility.detect(MinecraftVersion.parse("1.21.8"))
        assertIs<BukkitServerCompatibility.Supported>(result)
        assertEquals(BukkitServerCompatibility.Adapter.V1_21_6_TO_1_21_8, result.adapter)
    }

    @Test
    fun `rejects future versions instead of using latest classes`() {
        val result = BukkitServerCompatibility.detect(MinecraftVersion.parse("27.0"))
        assertIs<BukkitServerCompatibility.Unsupported>(result)
    }
}
