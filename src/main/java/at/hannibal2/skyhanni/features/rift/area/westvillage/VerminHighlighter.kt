package at.hannibal2.skyhanni.features.rift.area.westvillage

import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.baseMaxHealth
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntitySilverfish
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object VerminHighlighter {
    private val config get() = RiftAPI.config.area.westVillage.verminHighlight

    private val checkedEntities = TimeLimitedSet<Int>(1.minutes)

    private val VERMIN_FLY_TEXTURE by lazy { SkullTextureHolder.getTexture("VERMIN_FLY") }
    private val VERMIN_SPIDER_TEXTURE by lazy { SkullTextureHolder.getTexture("VERMIN_SPIDER") }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        for (entity in EntityUtils.getEntities<EntityLivingBase>()) {
            val id = entity.entityId
            if (id in checkedEntities) continue
            checkedEntities.add(id)

            if (!isVermin(entity)) continue
            val color = config.color.get().toChromaColor().withAlpha(60)
            RenderLivingEntityHelper.setEntityColorWithNoHurtTime(entity, color) { isEnabled() }
        }
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.color) {
            // running setEntityColorWithNoHurtTime() again
            checkedEntities.clear()
        }
    }

    private fun isVermin(entity: EntityLivingBase): Boolean = when (entity) {
        is EntityArmorStand -> entity.hasSkullTexture(VERMIN_FLY_TEXTURE) || entity.hasSkullTexture(VERMIN_SPIDER_TEXTURE)
        is EntitySilverfish -> entity.baseMaxHealth == 8

        else -> false
    }

    private fun hasItemInHand() = InventoryUtils.itemInHandId == "TURBOMAX_VACUUM".asInternalName()

    fun isEnabled() = RiftAPI.inRift() && RiftAPI.inWestVillage() && config.enabled && hasItemInHand()

}
