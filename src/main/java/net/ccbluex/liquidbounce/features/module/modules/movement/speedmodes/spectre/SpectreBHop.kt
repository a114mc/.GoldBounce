/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object SpectreBHop : SpeedMode("SpectreBHop") {
    override fun onMotion() {
        if (!mc.thePlayer.isMoving || mc.thePlayer.movementInput.jump) return
        if (mc.thePlayer.onGround) {
            strafe(1.1f)
            mc.thePlayer.motionY = 0.44
            return
        }
        strafe()
    }

}