/*
 * GoldBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/bzym2/GoldBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RotationUtils.isEntityHeightVisible
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.client.EntityLookup
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.extensions.minus
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.times
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexturedModalRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.text.get
import kotlin.text.toDouble
import kotlin.unaryMinus

object NameTags : Module("NameTags", Category.RENDER, hideModule = false) {

    private val typeValue = choices("Mode", arrayOf("3DTag", "2DTag"), "2DTag")

    private val renderSelf by boolean("RenderSelf", false)
    private val health by boolean("Health", true)
    private val healthFromScoreboard by boolean("HealthFromScoreboard", false) { health }
    private val absorption by boolean("Absorption", false) { health || healthBar }
    private val roundedHealth by boolean("RoundedHealth", true) { health }

    private val healthPrefix by boolean("HealthPrefix", false) { health }
    private val healthPrefixText by text("HealthPrefixText", "") { health && healthPrefix }

    private val healthSuffix by boolean("HealthSuffix", true) { health }
    private val healthSuffixText by text("HealthSuffixText", " ❤") { health && healthSuffix }

    private val indicator by boolean("Indicator", false)
    private val ping by boolean("Ping", false)
    private val healthBar by boolean("Bar", false)
    private val distance by boolean("Distance", false)
    private val armor by boolean("Armor", true)
    private val showArmorDurability by choices("Armor Durability", arrayOf("None", "Value", "Percentage"), "None") { armor }
    private val bot by boolean("Bots", true)
    private val potion by boolean("Potions", true)
    private val clearNames by boolean("ClearNames", false)
    private val font by font("Font", Fonts.font40)
    private val scale by float("Scale", 1F, 1F..4F)
    private val fontShadow by boolean("Shadow", true)

    private val background by boolean("Background", true)
    private val backgroundColorRed by int("Background-R", 0, 0..255) { background }
    private val backgroundColorGreen by int("Background-G", 0, 0..255) { background }
    private val backgroundColorBlue by int("Background-B", 0, 0..255) { background }
    private val backgroundColorAlpha by int("Background-Alpha", 70, 0..255) { background }

    private val border by boolean("Border", false)
    private val borderColorRed by int("Border-R", 0, 0..255) { border }
    private val borderColorGreen by int("Border-G", 0, 0..255) { border }
    private val borderColorBlue by int("Border-B", 0, 0..255) { border }
    private val borderColorAlpha by int("Border-Alpha", 100, 0..255) { border }

    private val maxRenderDistance by object : IntegerValue("MaxRenderDistance", 100, 1..200) {
        override fun onUpdate(value: Int) {
            maxRenderDistanceSq = value.toDouble().pow(2.0)
        }
    }

    private val onLook by boolean("OnLook", false)
    private val maxAngleDifference by float("MaxAngleDifference", 90f, 5.0f..90f) { onLook }

    private val thruBlocks by boolean("ThruBlocks", true)

    private var maxRenderDistanceSq = 0.0
        set(value) {
            field = if (value <= 0.0) maxRenderDistance.toDouble().pow(2.0) else value
        }

    private val inventoryBackground = ResourceLocation("textures/gui/container/inventory.png")
    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))

    // Cache for health color and formatted strings
    private var cachedHealthColor: Int = 0xFFFFFF
    private var cachedHealthPrefix = ""
    private var cachedHealthSuffix = ""

    private val entities by EntityLookup<EntityLivingBase>()
        .filter { bot || !isBot(it) }
        .filter { !onLook || EntityUtils.isLookingOnEntities(it, maxAngleDifference.toDouble()) }
        .filter { thruBlocks || isEntityHeightVisible(it) }
    @EventTarget
    fun onRender3D(event:Render3DEvent) {
        if (mc.theWorld == null || mc.thePlayer == null) return

        glPushAttrib(GL_ENABLE_BIT)
        glPushMatrix()

        // Disable lighting and depth test
        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)

        glEnable(GL_LINE_SMOOTH)

        // Enable blending
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        for (entity in entities) {
            val isRenderingSelf =
                entity is EntityPlayerSP && (mc.gameSettings.thirdPersonView != 0 || FreeCam.handleEvents())
            if (!isRenderingSelf || !renderSelf) {
                if (!EntityUtils.isSelected(entity, false)) continue
            }

            val name = entity.displayName.unformattedText ?: continue

            val distanceSquared = mc.thePlayer.getDistanceSqToEntity(entity)

            // In case user has FreeCam enabled, we restore the position back to normal,
            // so it renders the name-tag at the player's body position instead of the FreeCam position.
            if (isRenderingSelf) {
                FreeCam.restoreOriginalPosition()
            }

            if (distanceSquared <= maxRenderDistanceSq) {
                when (typeValue.get().lowercase(Locale.getDefault())) {
                    "2dtag" -> renderNameTag2D(entity, isRenderingSelf, if (clearNames) ColorUtils.stripColor(name) else name)
                    "3dtag" -> renderNameTag3D(entity, isRenderingSelf, if (clearNames) ColorUtils.stripColor(name) else name)
                }
                if (isRenderingSelf) {
                    FreeCam.useModifiedPosition()
                }
            }
        }

        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)

        glPopMatrix()
        glPopAttrib()

        // Reset color
        glColor4f(1F, 1F, 1F, 1F)
    }

    private fun renderNameTag2D(entity: EntityLivingBase, isRenderingSelf: Boolean, name: String) {
        var tag = name
        val fontRenderer = mc.fontRendererObj
        var scale = (mc.thePlayer.getDistanceToEntity(entity) / 2.5f).coerceAtLeast(4.0f)
        scale /= 200f
        tag = entity.displayName.formattedText

        var bot = ""
        bot = if (isBot(entity)) {
            "§7[Bot] "
        } else {
            ""
        }

        val healthText = if (health) " §a" + entity.health.toInt() + "" else ""
        val distanceText = if (distance) "§a[§f" + mc.thePlayer.getDistanceToEntity(entity).toInt() + "§a] " else ""
        val HEALTH: Int = entity.health.toInt()
        val COLOR1: String = when {
            HEALTH > 20 -> "§9"
            HEALTH >= 11 -> "§a"
            HEALTH >= 4 -> "§e"
            else -> "§4"
        }

        val hp = " [$COLOR1$HEALTH §c❤§f]"
        glPushMatrix()
        glTranslatef(
            (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks - mc.renderManager.renderPosX).toFloat(),
            (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks - mc.renderManager.renderPosY + entity.eyeHeight + 0.6).toFloat(),
            (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks - mc.renderManager.renderPosZ).toFloat()
        )

        glNormal3f(0.0f, 1.0f, 0.0f)
        glRotatef(-mc.renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        glRotatef(mc.renderManager.playerViewX, 1.0f, 0.0f, 0.0f)
        glScalef(-scale, -scale, scale)
        RenderUtils.setGLCap(GL_LIGHTING, false)
        RenderUtils.setGLCap(GL_DEPTH_TEST, false)
        RenderUtils.setGLCap(GL_BLEND, true)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        val text = distanceText + bot + tag + healthText + hp
        val stringWidth = fontRenderer.getStringWidth(text) / 2
        if (background) {
            Gui.drawRect((-stringWidth - 1), -14, (stringWidth + 1), -4, Integer.MIN_VALUE)
        }
        fontRenderer.drawString(text, (-stringWidth).toFloat(), (fontRenderer.FONT_HEIGHT - 22).toFloat(), 16777215, fontShadow)
        RenderUtils.revertAllCaps()
        glColor4f(1f, 1f, 1f, 1f)
        glPopMatrix()
    }

    private fun renderNameTag3D(entity: EntityLivingBase, isRenderingSelf: Boolean, name: String) {
        val thePlayer = mc.thePlayer ?: return

        // Set local fontRenderer
        val fontRenderer = font

        // Push matrix
        glPushMatrix()

        // Translate to player position
        val renderManager = mc.renderManager
        val rotateX = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f

        val (x, y, z) = entity.interpolatedPosition(entity.lastTickPos) - renderManager.renderPos
        glTranslated(x, y + entity.eyeHeight.toDouble() + 0.55, z)

        glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(renderManager.playerViewX * rotateX, 1F, 0F, 0F)

        // Disable lighting and depth test
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)

        // Enable blending
        enableGlCap(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // Determine friend or enemy status
        val isFriend = indicator && (entity is EntityPlayer && entity.isClientFriend())

        // Cache health color and formatted strings
        cachedHealthColor = calculateHealthColor(entity)
        cachedHealthPrefix = if (healthPrefix) healthPrefixText else ""
        cachedHealthSuffix = if (isFriend) " §9❤" else healthSuffixText

        // Modify tag
        val bot = isBot(entity)
        val nameColor = if (bot) "§3" else if (entity.isInvisible) "§6" else if (entity.isSneaking) "§4" else "§7"
        val symbolColor = if (isFriend) "§a" else "§c"
        val symbol = if (indicator) "$symbolColor✦" else ""

        val playerPing = if (entity is EntityPlayer) entity.getPing() else 0
        val playerDistance = thePlayer.getDistanceToEntity(entity)

        val distanceText = if (distance && !isRenderingSelf) "§7${playerDistance.roundToInt()} m " else ""
        val pingText = if (ping && entity is EntityPlayer) "§7[" + (if (playerPing > 200) "§c" else if (playerPing > 100) "§e" else "§a") + playerPing + "ms§7] " else ""
        val healthText = if (health) " " + getHealthString(entity) else ""
        val botText = if (bot) " §c§lBot" else ""

        val text = "$symbol $distanceText$pingText$nameColor$name$healthText$botText"

        // Scale based on distance
        val scale = ((playerDistance / 4F).coerceAtLeast(1F) / 150F) * scale

        glScalef(-scale, -scale, scale)

        val width = fontRenderer.getStringWidth(text) * 0.5f
        fontRenderer.drawString(
            text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, 0xFFFFFF, fontShadow
        )

        val dist = width + 4F - (-width - 2F)

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)

        val bgColor = if (background) {
            // Background
            Color(backgroundColorRed, backgroundColorGreen, backgroundColorBlue, backgroundColorAlpha)
        } else {
            // Transparent
            Color(0, 0, 0, 0)
        }

        val borderColor = Color(borderColorRed, borderColorGreen, borderColorBlue, borderColorAlpha)

        if (border) quickDrawBorderedRect(
            -width - 2F,
            -2F,
            width + 4F,
            fontRenderer.FONT_HEIGHT + 2F + if (healthBar) 2F else 0F,
            2F,
            borderColor.rgb,
            bgColor.rgb
        )
        else quickDrawRect(
            -width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F + if (healthBar) 2F else 0F, bgColor.rgb
        )

        if (healthBar) {
            quickDrawRect(
                -width - 2F,
                fontRenderer.FONT_HEIGHT + 3F,
                -width - 2F + dist,
                fontRenderer.FONT_HEIGHT + 4F,
                Color(50, 50, 50).rgb
            )
            quickDrawRect(
                -width - 2F,
                fontRenderer.FONT_HEIGHT + 3F,
                -width - 2F + (dist * (EntityUtils.getHealth(entity, healthFromScoreboard) / entity.maxHealth).coerceIn(0F, 1F)),
                fontRenderer.FONT_HEIGHT + 4F,
                cachedHealthColor
            )
        }

        glEnable(GL_TEXTURE_2D)

        fontRenderer.drawString(
            text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, Color.white.rgb, fontShadow
        )

        var foundPotion = false

        glEnable(GL_DEPTH_TEST)

        if (potion && entity is EntityPlayer) {
            val potions =
                entity.activePotionEffects.map { Potion.potionTypes[it.potionID] }
                    .filter { it.hasStatusIcon() }
            if (potions.isNotEmpty()) {
                foundPotion = true

                color(1.0F, 1.0F, 1.0F, 1.0F)
                disableLighting()
                enableTexture2D()

                val minX = (potions.size * -20) / 2

                glPushMatrix()
                enableRescaleNormal()
                for ((index, potion) in potions.withIndex()) {
                    color(1.0F, 1.0F, 1.0F, 1.0F)
                    mc.textureManager.bindTexture(inventoryBackground)
                    val i1 = potion.statusIconIndex
                    drawTexturedModalRect(minX + index * 20, -22, 0 + i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18, 0F)
                }
                disableRescaleNormal()
                glPopMatrix()

                enableAlpha()
                disableBlend()
                enableTexture2D()
            }
        }

        if (armor && entity is EntityPlayer) {
            RenderHelper.enableGUIStandardItemLighting()
            for (index in 0..4) {
                val itemStack = entity.getEquipmentInSlot(index) ?: continue

                mc.renderItem.zLevel = -147F
                mc.renderItem.renderItemAndEffectIntoGUI(
                    itemStack, -50 + index * 20, if (potion && foundPotion) -42 else -22
                )
            }
            RenderHelper.disableStandardItemLighting()

            enableAlpha()
            disableBlend()
            enableTexture2D()
        }


        if (showArmorDurability != "None" && entity is EntityPlayer) {

            val posX = if (potion && entity.activePotionEffects.isNotEmpty()) 10 else 12
            val posY = if (potion && entity.activePotionEffects.isNotEmpty()) -50 else -35

            val armorItems = entity.inventory.armorInventory
            var armorDurabilityText = ""

            for (item in armorItems) {
                if (item != null) {
                    val durabilityValue = item.maxDamage - item.itemDamage
                    val durabilityPercentage = (durabilityValue.toFloat() / item.maxDamage.toFloat()) * 100.0f

                    armorDurabilityText += when (showArmorDurability) {
                        "Value" -> " $durabilityValue"
                        "Percentage" -> " ${String.format("%.0f%%", durabilityPercentage)}"
                        else -> ""
                    }
                }
            }

            val width = fontRenderer.getStringWidth(armorDurabilityText.trim()) * 0.5f
            fontRenderer.drawString(
                armorDurabilityText.trim(),
                posX - width,
                posY.toFloat(),
                0xFFFFFF, fontShadow
            )
        }

        glDisable(GL_DEPTH_TEST)

        // Reset OpenGL caps
        resetCaps()

        // Reset color
        resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        // Pop matrix
        glPopMatrix()
    }

    private fun calculateHealthColor(entity: EntityLivingBase): Int {
        return when {
            entity.health <= 0 -> 0xFF0000
            else -> {
                val healthRatio = (EntityUtils.getHealth(entity, healthFromScoreboard, absorption) / entity.maxHealth).coerceIn(0.0F, 1.0F)
                val red = (255 * (1 - healthRatio)).toInt()
                val green = (255 * healthRatio).toInt()
                Color(red, green, 0).rgb
            }
        }
    }

    private fun getHealthString(entity: EntityLivingBase): String {
        return if (indicator) {
            val isFriend = entity is EntityPlayer && entity.isClientFriend()

            val friendColor = "§9"
            val enemyColor = "§c"

            val prefixColor = if (isFriend) friendColor else enemyColor
            val suffixColor = if (isFriend) friendColor else enemyColor

            val prefix = if (healthPrefix) "$prefixColor$healthPrefixText" else ""
            val suffix = if (healthSuffix) "$suffixColor$healthSuffixText" else ""

            val result = EntityUtils.getHealth(entity, healthFromScoreboard, absorption)

            val healthPercentage = (EntityUtils.getHealth(entity, healthFromScoreboard) / entity.maxHealth).coerceIn(0.0F, 1.0F)
            val healthColor = when {
                entity.health <= 0 -> enemyColor
                healthPercentage >= 0.75 -> friendColor
                healthPercentage >= 0.5 -> if (isFriend) "§e" else enemyColor
                healthPercentage >= 0.25 -> if (isFriend) "§6" else enemyColor
                else -> enemyColor
            }

            "$healthColor$prefix${if (roundedHealth) result.roundToInt() else decimalFormat.format(result)}$suffix"
        } else {
            val prefix = if (healthPrefix) healthPrefixText else ""
            val suffix = if (healthSuffix) healthSuffixText else ""

            val result = EntityUtils.getHealth(entity, healthFromScoreboard, absorption)

            val healthPercentage = (EntityUtils.getHealth(entity, healthFromScoreboard) / entity.maxHealth).coerceIn(0.0F, 1.0F)
            val healthColor = when {
                entity.health <= 0 -> "§4"
                healthPercentage >= 0.75 -> "§a"
                healthPercentage >= 0.5 -> "§e"
                healthPercentage >= 0.25 -> "§6"
                else -> "§c"
            }

            "$healthColor$prefix${if (roundedHealth) result.roundToInt() else decimalFormat.format(result)}$suffix"
        }
    }

    fun shouldRenderNameTags(entity: Entity) =
        handleEvents() && entity is EntityLivingBase && (ESP.handleEvents() && ESP.renderNameTags || EntityUtils.isSelected(
            entity,
            false
        ) && (bot || !isBot(entity)))

}
