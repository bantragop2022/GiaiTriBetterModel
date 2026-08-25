/*
 * This source file is part of BetterModel.
 * Copyright (c) 2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */

package kr.toxicity.model.bukkit

import kr.toxicity.model.api.version.MinecraftVersion

internal sealed interface BukkitServerCompatibility {
    val version: MinecraftVersion

    data class Supported(
        override val version: MinecraftVersion,
        val adapter: Adapter
    ) : BukkitServerCompatibility

    data class Unsupported(
        override val version: MinecraftVersion,
        val reason: String
    ) : BukkitServerCompatibility

    enum class Adapter(val displayName: String) {
        V1_21_4("1.21.4"),
        V1_21_5("1.21.5"),
        V1_21_6_TO_1_21_8("1.21.6-1.21.8"),
        V1_21_9_TO_1_21_10("1.21.9-1.21.10"),
        V1_21_11("1.21.11"),
        V26_1("26.1.x"),
        V26_2("26.2.x")
    }

    companion object {
        fun detect(version: MinecraftVersion): BukkitServerCompatibility {
            val adapter = when {
                version.major() == 1 && version.minor() == 21 -> when (version.patch()) {
                    4 -> Adapter.V1_21_4
                    5 -> Adapter.V1_21_5
                    in 6..8 -> Adapter.V1_21_6_TO_1_21_8
                    in 9..10 -> Adapter.V1_21_9_TO_1_21_10
                    11 -> Adapter.V1_21_11
                    else -> null
                }
                version.major() == 26 && version.minor() == 1 -> Adapter.V26_1
                version.major() == 26 && version.minor() == 2 -> Adapter.V26_2
                else -> null
            }
            return if (adapter != null) Supported(version, adapter) else Unsupported(
                version,
                when {
                    version.major() == 1 && version.minor() == 21 && version.patch() in 0..3 ->
                        "BetterModel v3 has no NMS, data-component, or resource-pack adapter for Minecraft 1.21.0-1.21.3."
                    else -> "No verified NMS and resource-pack adapter is bundled for this version."
                }
            )
        }
    }
}
