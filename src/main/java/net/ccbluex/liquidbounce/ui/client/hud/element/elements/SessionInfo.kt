package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.render.BlurUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.fade
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.skid.lbpp.SessionUtils
import net.ccbluex.liquidbounce.utils.skid.moonlight.render.ColorUtils.LiquidSlowly
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color

@ElementInfo(name = "SessionInfo")
class SessionInfo(x: Double = 10.0, y: Double = 10.0, scale: Float = 1F) : Element(x, y, scale) {
    private val colorModeValue =
        ListValue("Color", arrayOf("Custom", "Sky", "CRainbow", "LiquidSlowly", "Fade", "Mixer"), "Custom")
    private val modeValue = ListValue("Mode", arrayOf("1", "2", "3"), "1")
    val colorRedValue = IntegerValue("Red", 255, 0..255)
    val colorGreenValue = IntegerValue("Green", 255, 0..255)
    val colorBlueValue = IntegerValue("Blue", 255, 0..255)
    private val radiusValue = FloatValue("Radius", 4.25f, 0f..10f)

    val lineValue = BoolValue("Line", true)
    private val gradientAmountValue = IntegerValue("Gradient-Amount", 25, 1..50)
    private val saturationValue = FloatValue("Saturation", 0.9f, 0f..1f)
    private val brightnessValue = FloatValue("Brightness", 1f, 0f..1f)
    private val skyDistanceValue = IntegerValue("Sky-Distance", 2, -4..4)
    private val cRainbowSecValue = IntegerValue("CRainbow-Seconds", 2, 1..10)
    private val cRainbowDistValue = IntegerValue("CRainbow-Distance", 2, 1..6)
    private val mixerSecValue = IntegerValue("Mixer-Seconds", 2, 1..10)
    private val mixerDistValue = IntegerValue("Mixer-Distance", 2, 0..10)
    private val liquidSlowlyDistanceValue = IntegerValue("LiquidSlowly-Distance", 90, 1..90)
    private val fadeDistanceValue = IntegerValue("Fade-Distance", 50, 1..100)
    private val blurValue = BoolValue("Blur", false)
    private val blurStrength = IntegerValue("BlurStrength", 10, 1..60){ blurValue.get() }

    val counter = intArrayOf(0)

    override fun drawElement(): Border {
        val target: EntityPlayer? = mc.thePlayer
        val convertedTarget = target!!
        if (modeValue.get().equals("1")) {
            for (i in 0..(gradientAmountValue.get() - 1)) {
                RenderUtils.drawRoundedRect(0f, 0f, 165f, 63f, Color(0, 0, 0, 120).rgb,5f)
                getColor()?.let {
                    RenderUtils.drawRect(
                        0f, 12f, 165f, 13f,
                        it
                    )
                }
                Fonts.fontNoto35.drawString(
                    "SessionInfo",
                    165 / 2 - Fonts.fontNoto35.getStringWidth("SessionInfo") / 2F,
                    3F,
                    Color(0xFFFFFF).rgb
                )
                Fonts.font35.drawString("Playtime", 2F, 15F, Color(0xFFFFFF).rgb)
                Fonts.font35.drawString(
                    SessionUtils.getFormatSessionTime(),
                    165 - Fonts.font35.getStringWidth(SessionUtils.getFormatSessionTime()) - 3F,
                    15F,
                    Color(0xFFFFFF).rgb
                )
                Fonts.font35.drawString("Kills", 2F, 27F, Color(0xFFFFFF).rgb)
                Fonts.font35.drawString(
                    "${KillAura.CombatListener.killCounts}",
                    165 - Fonts.font35.getStringWidth("${KillAura.CombatListener.killCounts}") - 3F,
                    27F,
                    Color(0xFFFFFF).rgb
                )
                Fonts.font35.drawString("Name", 2F, 39F, Color(0xFFFFFF).rgb)
                Fonts.font35.drawString(
                    convertedTarget.name,
                    165 - Fonts.font35.getStringWidth(convertedTarget.name) - 3F,
                    39F,
                    Color(0xFFFFFF).rgb
                )
                Fonts.font35.drawString("Server", 2F, 51F, Color(0xFFFFFF).rgb)
                ServerUtils.serverData?.let {
                    Fonts.font35.drawString(
                        it.serverIP,
                        165 - Fonts.font35.getStringWidth(ServerUtils.grmip()) - 3F,
                        51F,
                        Color(0xFFFFFF).rgb
                    )
                }
            }
            return Border(0f, 0f, 165f, 63f)
        }
        if (modeValue.get().equals("2")) {
            for (i in 0..(gradientAmountValue.get() - 1)) {
                RenderUtils.drawRect(0f, 0f, 165f, 63f, Color(0, 0, 0, 120).rgb)
                getColor()?.let {
                    RenderUtils.drawRect(
                        0f, 0f, 165f, 1f,
                        it
                    )
                }
                Fonts.fontNoto35.drawString("SessionInfo", 2F, 4F, Color(0xFFFFFF).rgb)
                Fonts.font35.drawString("Playtime", 2F, 15F, Color(0xFFFFFF).rgb)
                Fonts.font35.drawString(
                    SessionUtils.getFormatSessionTime(),
                    165 - Fonts.font35.getStringWidth(SessionUtils.getFormatSessionTime()) - 3F,
                    15F,
                    Color(0xFFFFFF).rgb
                )
                Fonts.font35.drawString("Kills", 2F, 27F, Color(0xFFFFFF).rgb)
                Fonts.font35.drawString(
                    "${KillAura.CombatListener.killCounts}",
                    165 - Fonts.font35.getStringWidth("${KillAura.CombatListener.killCounts}") - 3F,
                    27F,
                    Color(0xFFFFFF).rgb
                )
                Fonts.font35.drawString("Name", 2F, 39F, Color(0xFFFFFF).rgb)
                Fonts.font35.drawString(
                    convertedTarget.name,
                    165 - Fonts.font35.getStringWidth(convertedTarget.name) - 3F,
                    39F,
                    Color(0xFFFFFF).rgb
                )
                Fonts.font35.drawString("Server", 2F, 51F, Color(0xFFFFFF).rgb)
                Fonts.font35.drawString(
                    ServerUtils.grmip(),
                    165 - Fonts.font35.getStringWidth(ServerUtils.grmip()) - 3F,
                    51F,
                    Color(0xFFFFFF).rgb
                )
                return Border(0f, 0f, 165f, 63f)
            }
        }
        if (modeValue.get().equals("3")) {
            val fontRenderer = Fonts.font35
            val y2 = fontRenderer.height * 4 + 11.0
            val x2 = 140.0

            var durationInMillis: Long = System.currentTimeMillis() - LiquidBounce.playTimeStart
            var second = durationInMillis / 1000 % 60
            var minute = durationInMillis / (1000 * 60) % 60
            var hour = durationInMillis / (1000 * 60 * 60) % 24
            var time: String
            time = String.format("%02dh %02dm %02ds", hour, minute, second)
            GlStateManager.pushMatrix()
            GL11.glTranslated(20.5, 15.5, 0.0)
            if (blurValue.get()) {
                BlurUtils.blurArea(
                    -14f,
                    -35f,
                    x2.toFloat() + 4,
                    y2.toFloat() - 10 + 10,
                    blurStrength.get().toFloat()
                )
            }
            RenderUtils.drawRoundedRect(
                -6f,
                -15f,
                x2.toFloat() + 4,
                y2.toFloat() - 10 + 10,
                Color(0, 0, 0, 120).rgb,
                radiusValue.get()
            )
            if (lineValue.get()) {
                val barLength = 142.toDouble()
                for (i in 0..(gradientAmountValue.get() - 1)) {
                    val barStart = i.toDouble() / gradientAmountValue.get().toDouble() * barLength
                    val barEnd = (i + 1).toDouble() / gradientAmountValue.get().toDouble() * barLength
                    getColor()?.let {
                        RenderUtils.drawGradientSideways(
                            -2.0 + barStart, -2.5, -2.0 + barEnd, -1.0,
                            it.rgb,
                            it.rgb
                        )
                    }
                }
            }

            Fonts.font35.drawStringWithShadow("Session Information", x2.toFloat() / 4f, -10f, Color.WHITE.rgb)
            Fonts.font35.drawStringWithShadow("Play Time: ", 2f, fontRenderer.height + -6f, Color.WHITE.rgb)
            Fonts.font35.drawStringWithShadow(time, 92f, fontRenderer.height + -6f, Color.WHITE.rgb)
            Fonts.font35.drawStringWithShadow("Player Killed ", 2f, fontRenderer.height * 2 + -4f, Color.WHITE.rgb)
            Fonts.font35.drawStringWithShadow(
                "" + KillAura.CombatListener.killCounts + "",
                135f,
                fontRenderer.height * 2 + -4f,
                Color.WHITE.rgb
            )
            Fonts.font35.drawStringWithShadow("GameWons", 2f, fontRenderer.height * 3 + -2f, Color.WHITE.rgb)
            Fonts.font35.drawStringWithShadow(
                "" + KillAura.CombatListener.win,
                135f,
                fontRenderer.height * 3 + -2f,
                Color.WHITE.rgb
            )
            Fonts.font35.drawStringWithShadow("Staff/Watchdog Bans", 2f, fontRenderer.height * 4 + 0f, Color.WHITE.rgb)
        }
        GlStateManager.popMatrix()
        return Border(14F, 0F, 165F, 63F)
    }

    fun getColor(): Color? {
        when (colorModeValue.get()) {
            "Custom" -> return Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())

            "LiquidSlowly" -> return LiquidSlowly(System.nanoTime(), 0, saturationValue.get(), brightnessValue.get())
            "Fade" -> return fade(Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get()), 0, 100)
        }
        return null
    }
}
