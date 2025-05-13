package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.value.*
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import java.awt.Color

@ElementInfo(name = "CPSCounter")
class CPSHudCounter : Element() {
    private val fontValue = FontValue("Fonts:", Fonts.minecraftFont)
    private val rightValue = BoolValue("Right Click", false)
    private val backGround = BoolValue("BackGround", true)
    override fun drawElement(): Border? {
        val font = fontValue.get()
        val string: String = if (backGround.get()) "CPS ${CPSCounter.getCPS(CPSCounter.MouseButton.LEFT)}" + if (rightValue.get()) " | ${CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT)}" else "" else "[CPS ${CPSCounter.getCPS(CPSCounter.MouseButton.LEFT)}" + if (rightValue.get()) " | ${CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT)}]" else "]"
        if (backGround.get()) {
            RenderUtils.drawRoundedRect(0F, 0F, 10F + font.getStringWidth(string), 10F + font.FONT_HEIGHT,Color(0,0,0,120).rgb, 5F)
        }
        if(backGround.get()) {
            GlowUtils.drawGlow(0F, 0F, 10F + font.getStringWidth(string), 10F + font.FONT_HEIGHT, 8, Color(0,0,0,120))
        }
        font.drawString(string, if (backGround.get()) 5F else 0F, if (backGround.get()) 6F else 0F, Color(255,255,255).rgb, true)
        return Border(0F, 0F, if (backGround.get()) 10F + font.getStringWidth(string) else font.getStringWidth(string).toFloat(), if (backGround.get()) 10F + font.FONT_HEIGHT else font.FONT_HEIGHT.toFloat())
    }
}