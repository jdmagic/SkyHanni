package at.hannibal2.skyhanni.features.bingo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.BingoJson.BingoTip
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.getOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BingoCardTips {
    private val config get() = SkyHanniMod.feature.event.bingo.bingoCard

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Bingo Card") return

        val itemName = event.itemStack?.name?.removeColor() ?: return

        val toolTip = event.toolTip
        val communityGoal = toolTip.getOrNull(1) == "§5§o§8Community Goal"
        val bingoTip: BingoTip = if (communityGoal) {
            BingoAPI.getCommunityTip(itemName) ?: return
        } else {
            BingoAPI.tips[itemName] ?: return
        }

        if (!communityGoal) {
            val difficulty = Difficulty.valueOf(bingoTip.difficulty.uppercase())
            toolTip[0] = toolTip[0] + " §7(" + difficulty.displayName + "§7)"
        }

        var index = if (!communityGoal) {
            toolTip.indexOf("§5§o§7Reward")
        } else {
            toolTip.indexOfFirst { it.startsWith("§5§o§7Contribution Rewards") }
        } - 1

        toolTip.add(index++, "")
        toolTip.add(index++, "§eGuide:")
        for (line in bingoTip.note) {
            toolTip.add(index++, " $line")
        }
        bingoTip.found?.let {
            toolTip.add(index++, "§7Found by: §e$it")
        }
    }

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!isEnabled()) return
        if (InventoryUtils.openInventoryName() != "Bingo Card") return

        val guiChest = event.gui
        val chest = guiChest.inventorySlots as ContainerChest
        for (slot in chest.inventorySlots) {
            if (slot == null) continue

            val goal = BingoAPI.personalGoals.firstOrNull { it.slot == slot.slotNumber } ?: continue
            if (config.hideDoneDifficulty && goal.done) continue

            BingoAPI.tips[goal.displayName]?.let {
                val difficulty = Difficulty.valueOf(it.difficulty.uppercase())
                slot highlight difficulty.color.addOpacity(120)
            }
        }
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.bingoSplashGuide

    enum class Difficulty(rawName: String, val color: LorenzColor) {
        EASY("Easy", LorenzColor.GREEN),
        MEDIUM("Medium", LorenzColor.YELLOW),
        HARD("Hard", LorenzColor.RED),
        ;

        val displayName = color.getChatColor() + rawName
    }
}
