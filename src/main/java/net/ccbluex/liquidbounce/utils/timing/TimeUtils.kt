/*
 * GoldBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/bzym2/GoldBounce/
 */
package net.ccbluex.liquidbounce.utils.timing

import net.ccbluex.liquidbounce.utils.extensions.safeDiv
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextInt
import kotlin.math.roundToInt

object TimeUtils {
    fun randomDelay(minDelay: Int, maxDelay: Int) = nextInt(minDelay, maxDelay + 1)

    fun randomClickDelay(minCPS: Int, maxCPS: Int): Int {
        val minDelay = 1000 safeDiv minCPS
        val maxDelay = 1000 safeDiv maxCPS
        return (Math.random() * (minDelay - maxDelay) + maxDelay).roundToInt()
    }
}