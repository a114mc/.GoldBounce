/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.Reach
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.RotationUtils.coerceBodyPoint
import net.ccbluex.liquidbounce.utils.RotationUtils.isFaced
import net.ccbluex.liquidbounce.utils.RotationUtils.performAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.boolean
import net.ccbluex.liquidbounce.value.float
import net.ccbluex.liquidbounce.value.int
import net.minecraft.entity.Entity
import java.util.*
import kotlin.math.atan

object Aimbot : Module("Aimbot", Category.COMBAT, hideModule = false) {

    private val range by float("Range", 4.4F, 1F..8F)
    private val horizontalAim by boolean("HorizontalAim", true)
    private val verticalAim by boolean("VerticalAim", true)
    private val legitimize by boolean("Legitimize", true) { horizontalAim || verticalAim }
    private val maxAngleChange by float("MaxAngleChange", 10f, 1F..180F) { horizontalAim || verticalAim }
    private val inViewMaxAngleChange by float("InViewMaxAngleChange", 35f, 1f..180f) { horizontalAim || verticalAim }
    private val predictClientMovement by int("PredictClientMovement", 2, 0..5)
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, -1f..2f)
    private val highestBodyPointToTargetValue: ListValue = object : ListValue(
        "HighestBodyPointToTarget",
        arrayOf("Head", "Body", "Feet"),
        "Head"
    ) {
        override fun isSupported() = verticalAim

        override fun onChange(oldValue: String, newValue: String): String {
            val newPoint = RotationUtils.BodyPoint.fromString(newValue)
            val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
            val coercedPoint = coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD)
            return coercedPoint.name
        }
    }
    private val highestBodyPointToTarget by highestBodyPointToTargetValue

    private val lowestBodyPointToTargetValue: ListValue = object : ListValue(
        "LowestBodyPointToTarget",
        arrayOf("Head", "Body", "Feet"),
        "Feet"
    ) {
        override fun isSupported() = verticalAim

        override fun onChange(oldValue: String, newValue: String): String {
            val newPoint = RotationUtils.BodyPoint.fromString(newValue)
            val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
            val coercedPoint = coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint)
            return coercedPoint.name
        }
    }

    private val lowestBodyPointToTarget by lowestBodyPointToTargetValue

    private val maxHorizontalBodySearch: FloatValue = object : FloatValue("MaxHorizontalBodySearch", 1f, 0f..1f) {
        override fun isSupported() = horizontalAim

        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalBodySearch.get())
    }

    private val minHorizontalBodySearch: FloatValue = object : FloatValue("MinHorizontalBodySearch", 0f, 0f..1f) {
        override fun isSupported() = horizontalAim

        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalBodySearch.get())
    }

    private val minRotationDifference by float(
        "MinRotationDifference",
        0f,
        0f..2f
    ) { verticalAim || horizontalAim }

    private val fov by float("FOV", 180F, 1F..180F)
    private val lock by boolean("Lock", true) { horizontalAim || verticalAim }
    private val onClick by boolean("OnClick", false) { horizontalAim || verticalAim }
    private val jitter by boolean("Jitter", false)
    private val yawJitterMultiplier by float("JitterYawMultiplier", 1f, 0.1f..2.5f)
    private val pitchJitterMultiplier by float("JitterPitchMultiplier", 1f, 0.1f..2.5f)
    private val center by boolean("Center", false)
    private val headLock by boolean("Headlock", false) { center && lock }
    private val headLockBlockHeight by float("HeadBlockHeight", -1f, -2f..0f) { headLock && center && lock }
    private val breakBlocks by boolean("BreakBlocks", true)

    private val clickTimer = MSTimer()

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.POST)
            return

        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return

        // Clicking delay
        if (mc.gameSettings.keyBindAttack.isKeyDown)
            clickTimer.reset()

        if (onClick && (clickTimer.hasTimePassed(150) || !mc.gameSettings.keyBindAttack.isKeyDown && AutoClicker.handleEvents()))
            return

        // Search for the best enemy to target
        val entity = theWorld.loadedEntityList.asSequence().mapNotNull { entity ->
            var isValid = false

            Backtrack.runWithNearestTrackedDistance(entity) {
                isValid = isSelected(entity, true) &&
                        thePlayer.canEntityBeSeen(entity) &&
                        thePlayer.getDistanceToEntityBox(entity) <= range &&
                        rotationDifference(entity) <= fov
            }

            entity.takeIf { isValid }
        }.minByOrNull { thePlayer.getDistanceToEntityBox(it) } ?: return

        // Should it always keep trying to lock on the enemy or just try to assist you?
        if (!lock && isFaced(entity, range.toDouble()))
            return

        val random = Random()

        var shouldReturn = false

        Backtrack.runWithNearestTrackedDistance(entity) {
            shouldReturn = !findRotation(entity, random)
        }

        if (shouldReturn)
            return

        // Jitter
        // Some players do jitter on their mouses causing them to shake around. This is trying to simulate this behavior.
        if (jitter) {
            if (random.nextBoolean()) {
                thePlayer.fixedSensitivityYaw += ((random.nextGaussian() - 0.5f) * yawJitterMultiplier).toFloat()
            }

            if (random.nextBoolean()) {
                thePlayer.fixedSensitivityPitch += ((random.nextGaussian() - 0.5f) * pitchJitterMultiplier).toFloat()
            }
        }
    }

    private fun findRotation(entity: Entity, random: Random): Boolean {
        val player = mc.thePlayer ?: return false

        if (mc.playerController.isHittingBlock && breakBlocks) {
            return false
        }

        val (predictX, predictY, predictZ) = entity.currPos.subtract(entity.prevPos)
            .times(2 + predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(predictX, predictY, predictZ)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(player.movementInput)

        repeat(predictClientMovement + 1) {
            simPlayer.tick()
        }

        player.setPosAndPrevPos(simPlayer.pos)

        val playerRotation = player.rotation

        val destinationRotation = if (center) {
            toRotation(boundingBox.center, true)
        } else {
            searchCenter(
                boundingBox,
                outborder = false,
                predict = true,
                lookRange = range,
                attackRange = if (Reach.handleEvents()) Reach.combatReach else 3f,
                bodyPoints = listOf(highestBodyPointToTarget, lowestBodyPointToTarget),
                horizontalSearch = minHorizontalBodySearch.get()..maxHorizontalBodySearch.get(),
            )
        }

        if (destinationRotation == null) {
            player.setPosAndPrevPos(currPos, oldPos)
            return false
        }

        // look headLockBlockHeight higher
        if (headLock && center && lock) {
            val distance = player.getDistanceToEntityBox(entity)
            val playerEyeHeight = player.eyeHeight
            val blockHeight = headLockBlockHeight

            // Calculate the pitch offset needed to shift the view one block up
            val pitchOffset = Math.toDegrees(atan((blockHeight + playerEyeHeight) / distance)).toFloat()

            destinationRotation.pitch -= pitchOffset
        }

        // Figure out the best turn speed suitable for the distance and configured turn speed
        val rotationDiff = rotationDifference(playerRotation, destinationRotation)

        // is enemy visible to player on screen. Fov is about to be right with that you can actually see on the screen. Still not 100% accurate, but it is fast check.
        val supposedTurnSpeed = if (rotationDiff < mc.gameSettings.fovSetting) {
            inViewMaxAngleChange
        } else {
            maxAngleChange
        }

        val gaussian = random.nextGaussian()

        val realisticTurnSpeed = rotationDiff * ((supposedTurnSpeed + (gaussian - 0.5)) / 180)

        // Directly access performAngleChange since this module does not use RotationSettings
        val rotation = performAngleChange(
            player.rotation,
            destinationRotation,
            realisticTurnSpeed.toFloat(),
            legitimize = legitimize,
            minRotationDiff = minRotationDifference,
        )

        rotation.toPlayer(player, horizontalAim, verticalAim)

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }
}