/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils.airTicks
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.potion.Potion

object VerusLowHop : SpeedMode("VerusLowHop") {

    private var speed = 0.0f

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        if (player.isInWater || player.isInLava || player.isInWeb || player.isOnLadder) return
        
        if (player.isMoving) {
            if (player.onGround) {
                speed = if (player.isPotionActive(Potion.moveSpeed)
                    && player.getActivePotionEffect(Potion.moveSpeed).amplifier >= 1)
                        0.5f else 0.36f

                player.tryJump()
            } else {
                if (airTicks <= 1) {
                    player.motionY = -0.09800000190734863
                }

                speed *= 0.98f
            }

            strafe(speed)
        }
    }
}
