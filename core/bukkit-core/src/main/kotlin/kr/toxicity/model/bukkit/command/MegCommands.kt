/**
 * This source file is part of BetterModel.
 * Copyright (c) 2024–2026 toxicity188
 * Licensed under the MIT License.
 * See LICENSE.md file for full license text.
 */
package kr.toxicity.model.bukkit.command

import kr.toxicity.model.api.BetterModel
import kr.toxicity.model.bukkit.audience.AudiencePlayer
import kr.toxicity.model.bukkit.util.PLUGIN
import kr.toxicity.model.bukkit.util.wrap
import kr.toxicity.model.command.nullable
import kr.toxicity.model.command.register
import kr.toxicity.model.util.PLATFORM
import kr.toxicity.model.util.info
import kr.toxicity.model.util.toComponent
import kr.toxicity.model.util.warn
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.NamedTextColor.GREEN
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.persistence.PersistentDataType
import org.incendo.cloud.CommandManager
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.parser.standard.IntegerParser.integerParser
import org.incendo.cloud.parser.standard.StringParser.stringParser
import org.incendo.cloud.suggestion.SuggestionProvider.blockingStrings
import java.io.File

private val MEG_HOST_KEY by lazy { NamespacedKey(PLUGIN, "meg_summon_host") }
private val MEG_MODEL_SUGGESTION = blockingStrings<Audience> { _, _ ->
    (BetterModel.modelKeys() + ModelDiskScanner.scan().byMid.keys).sorted()
}

internal fun CommandManager<Audience>.startMegCommand() {
    register("meg", "GiaiTri model administration commands.", { it }, "giaitrimodel") {
        create("summon", "Summons a BetterModel test host.") {
            required("mid", stringParser(), MEG_MODEL_SUGGESTION)
                .senderType(AudiencePlayer::class.java)
                .handler(::megSummon)
        }
        create("list", "Lists model identifiers on disk.") {
            optional("page", integerParser(1, Int.MAX_VALUE)).handler(::megList)
        }
        create("info", "Shows real data for a model identifier.") {
            required("mid", stringParser(), MEG_MODEL_SUGGESTION).handler(::megInfo)
        }
        create("diag", "Shows BetterModel and model disk diagnostics.") {
            handler(::megDiag)
        }
        create("reload", "Reloads BetterModel.") {
            handler(::reload)
        }
        create("clear", "Removes only hosts created by /meg summon.") {
            handler(::megClear)
        }
    }
    ModelDiskScanner.logDuplicates()
}

private fun megSummon(context: CommandContext<AudiencePlayer>) {
    val audience = context.sender()
    val mid = context.get<String>("mid")
    val model = BetterModel.modelOrNull(mid) ?: return audience.warn("Unknown model: $mid")
    val stand = audience.sender.world.spawn(audience.sender.location, ArmorStand::class.java) {
        it.isInvisible = true
        it.isInvulnerable = true
        it.isSilent = true
        it.isPersistent = true
        it.setGravity(false)
        it.isMarker = true
        it.persistentDataContainer.set(MEG_HOST_KEY, PersistentDataType.BYTE, 1.toByte())
    }
    runCatching { model.create(stand.wrap()) }
        .onSuccess { audience.info("Summoned model: $mid") }
        .onFailure {
            stand.remove()
            audience.warn("Unable to summon model: $mid (${it.message ?: it.javaClass.simpleName})")
        }
}

private fun megClear(context: CommandContext<Audience>) {
    var removed = 0
    Bukkit.getWorlds().forEach { world ->
        world.entities.filter { it.persistentDataContainer.has(MEG_HOST_KEY, PersistentDataType.BYTE) }.forEach {
            it.remove()
            removed++
        }
    }
    context.sender().info("Removed $removed /meg summon host(s).")
}

private fun megList(context: CommandContext<Audience>) {
    val scan = ModelDiskScanner.scan()
    val mids = (scan.byMid.keys + BetterModel.modelKeys()).distinct().sorted()
    val pageSize = 10
    val maxPage = maxOf(1, (mids.size + pageSize - 1) / pageSize)
    val page = context.nullable("page", 1).coerceIn(1, maxPage)
    context.sender().info("Models (${mids.size}) - page $page/$maxPage".toComponent(YELLOW))
    mids.drop((page - 1) * pageSize).take(pageSize).forEach { mid ->
        val registered = BetterModel.modelOrNull(mid) != null
        context.sender().info("$mid ${if (registered) "[registered]" else "[disk only]"}".toComponent(if (registered) GREEN else GRAY))
    }
}

private fun megInfo(context: CommandContext<Audience>) {
    val mid = context.get<String>("mid")
    val scan = ModelDiskScanner.scan()
    val model = BetterModel.modelOrNull(mid)
    val files = scan.byMid[mid].orEmpty()
    if (model == null && files.isEmpty()) return context.sender().warn("Unknown model: $mid")
    val lines = mutableListOf("MID: $mid", "Registered: ${model != null}")
    model?.let {
        lines += "Animations: ${it.animations().keys.sorted().joinToString().ifEmpty { "none" }}"
        lines += "Bones: ${it.flatten().count()}"
        lines += "Hitboxes: ${it.flatten().filter { group -> group.hitBox != null }.count()}"
    }
    lines += "Source: ${files.joinToString { it.relativeTo(PLUGIN.dataFolder).path }.ifEmpty { "unknown" }}"
    lines.forEach(context.sender()::info)
}

private fun megDiag(context: CommandContext<Audience>) {
    val scan = ModelDiskScanner.scan()
    val registered = BetterModel.modelKeys().toSet()
    val diskOnly = scan.byMid.keys - registered
    listOf(
        "BetterModel version: ${PLATFORM.semver()}",
        "Paper version: ${Bukkit.getVersion()}",
        "Java version: ${System.getProperty("java.version")}",
        ".bbmodel files on disk: ${scan.files.size}",
        "Unique MID: ${scan.byMid.size}",
        "Duplicate MID: ${scan.duplicates.size}",
        "Registered models: ${registered.size}",
        "Disk models not registered: ${diskOnly.size}${diskOnly.sorted().take(10).joinToString(prefix = if (diskOnly.isEmpty()) "" else " (", postfix = if (diskOnly.isEmpty()) "" else ")")}",
    ).forEach(context.sender()::info)
    scan.duplicates.forEach { (mid, files) ->
        context.sender().warn("Duplicate MID '$mid': ${files.joinToString { it.relativeTo(PLUGIN.dataFolder).path }}")
    }
}

private object ModelDiskScanner {
    data class Result(val files: List<File>, val byMid: Map<String, List<File>>) {
        val duplicates = byMid.filterValues { it.size > 1 }
    }

    fun scan(): Result {
        val root = File(PLUGIN.dataFolder, "models")
        val files = root.walkTopDown().filter { it.isFile && it.extension.equals("bbmodel", ignoreCase = true) }.sortedBy(File::getPath).toList()
        return Result(files, files.groupBy { it.nameWithoutExtension.lowercase() })
    }

    fun logDuplicates() = scan().duplicates.forEach { (mid, files) ->
        PLUGIN.logger.warning("Duplicate model MID '$mid': ${files.joinToString { it.path }}")
    }
}
