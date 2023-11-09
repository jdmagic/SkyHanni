package at.hannibal2.skyhanni.features.event.jerry.frozentreasure

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addOrPut
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.concurrent.fixedRateTimer

object FrozenTreasureTracker {
    private val config get() = SkyHanniMod.feature.event.winter.frozenTreasureTracker
    private var display = emptyList<List<Any>>()
    private var estimatedIce = 0L
    private var lastEstimatedIce = 0L
    private var icePerSecond = mutableListOf<Long>()
    private var icePerHour = 0
    private var stoppedChecks = 0
    private var compactPattern = "COMPACT! You found an Enchanted Ice!".toPattern()
    private val tracker = SkyHanniTracker("Frozen Treasure Tracker", { Data() }, { it.frozenTreasureTracker })
    { saveAndUpdate() }

    init {
        fixedRateTimer(name = "skyhanni-frozen-treasure-tracker", period = 1000) {
            if (!onJerryWorkshop()) return@fixedRateTimer
            calculateIcePerHour()
        }
    }

    class Data : TrackerData() {

        override fun reset() {
            treasureCount.clear()
            treasuresMined = 0
            compactProcs = 0
        }

        @Expose
        var treasuresMined = 0

        @Expose
        var compactProcs = 0

        @Expose
        var treasureCount: MutableMap<FrozenTreasure, Int> = mutableMapOf()
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        icePerHour = 0
        stoppedChecks = 0
        icePerSecond = mutableListOf()
        saveAndUpdate()
    }

    private fun calculateIcePerHour() {
        val difference = estimatedIce - lastEstimatedIce
        lastEstimatedIce = estimatedIce

        if (difference == estimatedIce) return


        if (difference == 0L) {
            if (icePerSecond.isEmpty()) return
            stoppedChecks += 1
        } else {
            if (stoppedChecks > 60) {
                stoppedChecks = 0
                icePerSecond.clear()
                icePerHour = 0
            }
            while (stoppedChecks > 0) {
                stoppedChecks -= 1
                icePerSecond.add(0)
            }
            icePerSecond.add(difference)
            val listCopy = icePerSecond
            while (listCopy.size > 1200) listCopy.removeAt(0)
            icePerSecond = listCopy
        }
        icePerHour = (icePerSecond.average() * 3600).toInt()
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat) {
            newList.add(map[index])
            if (newList.size == 1) {
                tracker.addDisplayModeToggle(newList)
            }
        }
        tracker.addSessionResetButton(newList)
        return newList
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!ProfileStorageData.loaded) return
        if (!onJerryWorkshop()) return

        val message = event.message.removeColor().trim()

        compactPattern.matchMatcher(message) {
            tracker.modify {
                it.compactProcs += 1
            }
            saveAndUpdate()
            if (config.hideMessages) event.blockedReason = "frozen treasure tracker"
        }

        for (treasure in FrozenTreasure.entries) {
            if ("FROZEN TREASURE! You found ${treasure.displayName.removeColor()}!".toRegex().matches(message)) {
                tracker.modify {
                    it.treasuresMined += 1
                    it.treasureCount.addOrPut(treasure, 1)
                }
                saveAndUpdate()
                if (config.hideMessages) event.blockedReason = "frozen treasure tracker"
            }
        }
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }

    private fun drawTreasureDisplay(data: Data) = buildList<List<Any>> {
        addAsSingletonList("§1§lFrozen Treasure Tracker")
        addAsSingletonList("§6${formatNumber(data.treasuresMined)} Treasures Mined")
        addAsSingletonList("§3${formatNumber(estimatedIce)} Total Ice")
        addAsSingletonList("§3${formatNumber(icePerHour)} Ice/hr")
        addAsSingletonList("§8${formatNumber(data.treasuresMined)} Compact Procs")
        addAsSingletonList("")

        for (treasure in FrozenTreasure.entries) {
            val count = (data.treasureCount[treasure] ?: 0) * if (config.showAsDrops) treasure.defaultAmount else 1
            addAsSingletonList("§b${formatNumber(count)} ${treasure.displayName}")
        }
        addAsSingletonList("")
    }

    fun formatNumber(amount: Number): String {
        if (amount is Int) return amount.addSeparators()
        if (amount is Long) return NumberUtil.format(amount)
        return "$amount"
    }

    private fun saveAndUpdate() {
        val data = tracker.currentDisplay() ?: return
        calculateIce(data)
        display = formatDisplay(drawTreasureDisplay(data))
    }

    private fun calculateIce(data: Data) {
        estimatedIce = data.compactProcs * 160L
        for (treasure in FrozenTreasure.entries) {
            val amount = data.treasureCount[treasure] ?: 0
            estimatedIce += amount * treasure.defaultAmount * treasure.iceMultiplier
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!config.enabled) return
        if (!onJerryWorkshop()) return
        if (config.onlyInCave && !inGlacialCave()) return

        tracker.renderDisplay(config.position, display)
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.frozenTreasureTracker", "event.winter.frozenTreasureTracker")
    }

    private fun onJerryWorkshop() = IslandType.WINTER.isInIsland()

    private fun inGlacialCave() =
        onJerryWorkshop() && ScoreboardData.sidebarLinesFormatted.contains(" §7⏣ §3Glacial Cave")

    fun resetCommand(args: Array<String>) {
        tracker.resetCommand(args, "shresetfrozentreasuretracker")
    }
}
