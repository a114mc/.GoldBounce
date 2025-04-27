package net.ccbluex.liquidbounce.utils.skid.crosssine

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.block.BlockSign
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.collections.plusAssign
import kotlin.compareTo
import kotlin.math.*


object MovementUtils : net.ccbluex.liquidbounce.utils.MinecraftInstance() {

    fun resetMotion(y: Boolean) {
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        if (y) mc.thePlayer.motionY = 0.0
    }
    fun predictedMotion(motion: Double): Double {
        return (motion - 0.08) * 0.98f
    }
    fun getFallDistance(entity: Entity): Double {
        var fallDist = -1.0
        val pos: Vec3 = Vec3(entity.posX, entity.posY, entity.posZ)
        var y = floor(pos.yCoord).toInt()
        if ((pos.yCoord % 1).toInt() == 0) y--
        for (i in y downTo -1 + 1) {
            val block: Block? = getBlock(BlockPos(floor(pos.xCoord).toInt() , i, floor(pos.zCoord).toInt()))
            if (block !is BlockAir && block !is BlockSign) {
                fallDist = (y - i).toDouble()
                break
            }
        }
        return fallDist
    }
    fun predictedMotion(motion: Double, ticks: Int): Double {
        if (ticks == 0) return motion
        var predicted = motion

        for (i in 0 until ticks) {
            predicted = (predicted - 0.08) * 0.98f
        }

        return predicted
    }

    fun getSpeed(): Float {
        return sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ).toFloat()
    }
    fun setSpeed(speed: Float) {
        val currentSpeed = getSpeed()
        if (currentSpeed > 0.0f) {
            val factor = speed / currentSpeed
            mc.thePlayer.motionX *= factor
            mc.thePlayer.motionZ *= factor
        }
    }

    fun FlyBasic(speed: Float) {
        if (mc.gameSettings.keyBindJump.isKeyDown) mc.thePlayer.motionY += speed

        if (mc.gameSettings.keyBindSneak.isKeyDown) mc.thePlayer.motionY -= speed

        net.ccbluex.liquidbounce.utils.MovementUtils.strafe(speed)
    }
    fun jump(checkSpeed: Boolean, motion: Boolean = false, motionY: Double = 0.42) {
        if (!mc.gameSettings.keyBindJump.isKeyDown && (!checkSpeed || !Speed.state)) {
            if (motion) {
                mc.thePlayer.motionY = motionY
            } else {
                mc.thePlayer.jump()
            }
        }
    }
    fun jump(event: MoveEvent) {
        var jumpY = mc.thePlayer.jumpMovementFactor.toDouble()
        if (mc.thePlayer.isPotionActive(Potion.jump)) {
            jumpY += ((mc.thePlayer.getActivePotionEffect(Potion.jump).amplifier + 1).toFloat() * 0.1f).toDouble()
        }
        mc.thePlayer.motionY = jumpY
        event.y = mc.thePlayer.motionY
    }
    /**
     * Calculate speed based on the speed potion effect level/amplifier
     */
    fun getSpeedWithPotionEffects(speed: Double) =
        mc.thePlayer.getActivePotionEffect(Potion.moveSpeed)?.let {
            speed * (1 + (it.amplifier + 1) * 0.2)
        } ?: speed

    fun strafe() {
        net.ccbluex.liquidbounce.utils.MovementUtils.strafe(getSpeed())
    }

    fun move() {
        move(getSpeed())
    }
    fun getSpeedAmplifier(): Int {
        return if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) 1 + mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier else 0
    }
    fun isMoving(): Boolean {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveForward != 0f || mc.thePlayer.movementInput.moveStrafe != 0f)
    }

    fun isStrafing(): Boolean {
        return mc.thePlayer != null && (mc.thePlayer.movementInput.moveStrafe != 0f)
    }

    fun hasMotion(): Boolean {
        return mc.thePlayer.motionX != 0.0 && mc.thePlayer.motionZ != 0.0 && mc.thePlayer.motionY != 0.0
    }

    fun strafe(speed: Float) {
        if (!net.ccbluex.liquidbounce.utils.MovementUtils.isMoving()) return
        mc.thePlayer.motionX = -sin(net.ccbluex.liquidbounce.utils.MovementUtils.direction) * speed
        mc.thePlayer.motionZ = cos(net.ccbluex.liquidbounce.utils.MovementUtils.direction) * speed
    }
    fun strafe(event: MoveEvent, speed: Float) {
        if (!net.ccbluex.liquidbounce.utils.MovementUtils.isMoving()) return
       event.x = -sin(net.ccbluex.liquidbounce.utils.MovementUtils.direction) * speed
       event.z = cos(net.ccbluex.liquidbounce.utils.MovementUtils.direction) * speed
    }

    fun strafe(speed: Double) {
        if (!net.ccbluex.liquidbounce.utils.MovementUtils.isMoving()) return
        mc.thePlayer.motionX = -sin(net.ccbluex.liquidbounce.utils.MovementUtils.direction) * speed
        mc.thePlayer.motionZ = cos(net.ccbluex.liquidbounce.utils.MovementUtils.direction) * speed
    }

    fun defaultSpeed(): Double {
        var baseSpeed = 0.2873
        if (Minecraft.getMinecraft().thePlayer.isPotionActive(Potion.moveSpeed)) {
            val amplifier = Minecraft.getMinecraft().thePlayer.getActivePotionEffect(Potion.moveSpeed)
                .amplifier
            baseSpeed *= 1.0 + 0.2 * (amplifier + 1)
        }
        return baseSpeed
    }
    fun getSpeed(e: EntityLivingBase): Float {
        return sqrt((e.posX - e.prevPosX) * (e.posX - e.prevPosX) + (e.posZ - e.prevPosZ) * (e.posZ - e.prevPosZ))
            .toFloat()
    }

    fun doTargetStrafe(
        curTarget: EntityLivingBase,
        direction_: Float,
        radius: Float,
        moveEvent: MoveEvent,
        mathRadius: Int = 0
    ) {
        if (!net.ccbluex.liquidbounce.utils.MovementUtils.isMoving()) return

        var forward_ = 0.0
        var strafe_ = 0.0
        val speed_ = sqrt(moveEvent.x * moveEvent.x + moveEvent.z * moveEvent.z)

        if (speed_ <= 0.0001)
            return

        var _direction = 0.0
        if (direction_ > 0.001) {
            _direction = 1.0
        } else if (direction_ < -0.001) {
            _direction = -1.0
        }
        var curDistance = (0.01).toFloat()
        if (mathRadius == 1) {
            curDistance = mc.thePlayer.getDistanceToEntity(curTarget)
        } else if (mathRadius == 0) {
            curDistance =
                sqrt((mc.thePlayer.posX - curTarget.posX) * (mc.thePlayer.posX - curTarget.posX) + (mc.thePlayer.posZ - curTarget.posZ) * (mc.thePlayer.posZ - curTarget.posZ)).toFloat()
        }
        if (curDistance < radius - speed_) {
            forward_ = -1.0
        } else if (curDistance > radius + speed_) {
            forward_ = 1.0
        } else {
            forward_ = (curDistance - radius) / speed_
        }
        if (curDistance < radius + speed_ * 2 && curDistance > radius - speed_ * 2) {
            strafe_ = 1.0
        }
        strafe_ *= _direction
        var strafeYaw = net.ccbluex.liquidbounce.utils.RotationUtils.getRotationsEntity(curTarget).yaw.toDouble()
        val covert_ = sqrt(forward_ * forward_ + strafe_ * strafe_)

        forward_ /= covert_
        strafe_ /= covert_
        var turnAngle = Math.toDegrees(asin(strafe_))
        if (turnAngle > 0) {
            if (forward_ < 0)
                turnAngle = 180F - turnAngle
        } else {
            if (forward_ < 0)
                turnAngle = -180F - turnAngle
        }
        strafeYaw = Math.toRadians((strafeYaw + turnAngle))
        moveEvent.x = -sin(strafeYaw) * speed_
        moveEvent.z = cos(strafeYaw) * speed_
        mc.thePlayer.motionX = moveEvent.x
        mc.thePlayer.motionZ = moveEvent.z
    }

    fun move(speed: Float) {
        if (!net.ccbluex.liquidbounce.utils.MovementUtils.isMoving()) return
        val yaw = net.ccbluex.liquidbounce.utils.MovementUtils.direction
        mc.thePlayer.motionX += -sin(yaw) * speed
        mc.thePlayer.motionZ += cos(yaw) * speed
    }


    fun JumpBoost(motionY: Double): Double {
        return if (mc.thePlayer.isPotionActive(Potion.jump)) {
            motionY + (mc.thePlayer.getActivePotionEffect(Potion.jump).amplifier + 1) * 0.1f
        } else motionY
    }

    fun jumpMotion(): Double {
        return JumpBoost(0.42)
    }

    fun limitSpeed(speed: Float) {
        val yaw = net.ccbluex.liquidbounce.utils.MovementUtils.direction
        val maxXSpeed = -sin(yaw) * speed
        val maxZSpeed = cos(yaw) * speed
        if (mc.thePlayer.motionX > maxZSpeed) {
            mc.thePlayer.motionX = maxXSpeed
        }
        if (mc.thePlayer.motionZ > maxZSpeed) {
            mc.thePlayer.motionZ = maxZSpeed
        }
    }

    /**
     * make player move slowly like when using item
     * @author liulihaocai
     */
    fun limitSpeedByPercent(percent: Float) {
        mc.thePlayer.motionX *= percent
        mc.thePlayer.motionZ *= percent
    }

    fun forward(length: Double) {
        val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
        mc.thePlayer.setPosition(
            mc.thePlayer.posX + -sin(yaw) * length,
            mc.thePlayer.posY,
            mc.thePlayer.posZ + cos(yaw) * length
        )
    }

    val direction: Double
        get() {
            var rotationYaw = mc.thePlayer.rotationYaw
            if (mc.thePlayer.moveForward < 0f) rotationYaw += 180f
            var forward = 1f
            if (mc.thePlayer.moveForward < 0f) forward = -0.5f else if (mc.thePlayer.moveForward > 0f) forward = 0.5f
            if (mc.thePlayer.moveStrafing > 0f) rotationYaw -= 90f * forward
            if (mc.thePlayer.moveStrafing < 0f) rotationYaw += 90f * forward
            return Math.toRadians(rotationYaw.toDouble())
        }
    val jumpMotion: Float
        get() {
            var mot = 0.42f
            if (mc.thePlayer.isPotionActive(Potion.jump)) {
                mot += (mc.thePlayer.getActivePotionEffect(Potion.jump).amplifier + 1).toFloat() * 0.1f
            }
            return mot
        }

    val movingYaw: Float
        get() = (net.ccbluex.liquidbounce.utils.MovementUtils.direction * 180f / Math.PI).toFloat()

    var bps = 0.0
        private set
    private var lastX = 0.0
    private var lastY = 0.0
    private var lastZ = 0.0

    fun setMotion(speed: Double) {
        var forward = mc.thePlayer.movementInput.moveForward.toDouble()
        var strafe = mc.thePlayer.movementInput.moveStrafe.toDouble()
        var yaw = mc.thePlayer.rotationYaw
        if (forward == 0.0 && strafe == 0.0) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
        } else {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += (if (forward > 0.0) -45 else 45).toFloat()
                } else if (strafe < 0.0) {
                    yaw += (if (forward > 0.0) 45 else -45).toFloat()
                }
                strafe = 0.0
                if (forward > 0.0) {
                    forward = 1.0
                } else if (forward < 0.0) {
                    forward = -1.0
                }
            }
            val cos = cos(Math.toRadians((yaw + 90.0f).toDouble()))
            val sin = sin(Math.toRadians((yaw + 90.0f).toDouble()))
            mc.thePlayer.motionX = (forward * speed * cos +
                    strafe * speed * sin)
            mc.thePlayer.motionZ = (forward * speed * sin -
                    strafe * speed * cos)
        }
    }

    fun updateBlocksPerSecond() {
        if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 1) {
            bps = 0.0
        }
        val distance = mc.thePlayer.getDistance(lastX, lastY, lastZ)
        lastX = mc.thePlayer.posX
        lastY = mc.thePlayer.posY
        lastZ = mc.thePlayer.posZ
        bps = distance * (20 * mc.timer.timerSpeed)
    }

    fun setSpeed(
        moveEvent: MoveEvent,
        moveSpeed: Double,
        pseudoYaw: Float,
        pseudoStrafe: Double,
        pseudoForward: Double
    ) {
        var forward = pseudoForward
        var strafe = pseudoStrafe
        var yaw = pseudoYaw
        if (forward == 0.0 && strafe == 0.0) {
            moveEvent.z = 0.0
            moveEvent.x = 0.0
        } else {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += (if (forward > 0.0) -45 else 45).toFloat()
                } else if (strafe < 0.0) {
                    yaw += (if (forward > 0.0) 45 else -45).toFloat()
                }
                strafe = 0.0
                if (forward > 0.0) {
                    forward = 1.0
                } else if (forward < 0.0) {
                    forward = -1.0
                }
            }
            val cos = cos(Math.toRadians((yaw + 90.0f).toDouble()))
            val sin = sin(Math.toRadians((yaw + 90.0f).toDouble()))
            moveEvent.x = forward * moveSpeed * cos + strafe * moveSpeed * sin
            moveEvent.z = forward * moveSpeed * sin - strafe * moveSpeed * cos
        }
    }

    private fun calculateGround(): Double {
        val playerBoundingBox = mc.thePlayer.entityBoundingBox
        var blockHeight = 1.0
        var ground = mc.thePlayer.posY
        while (ground > 0.0) {
            val customBox = AxisAlignedBB(
                playerBoundingBox.maxX,
                ground + blockHeight,
                playerBoundingBox.maxZ,
                playerBoundingBox.minX,
                ground,
                playerBoundingBox.minZ
            )
            if (mc.theWorld.checkBlockCollision(customBox)) {
                if (blockHeight <= 0.05) return ground + blockHeight
                ground += blockHeight
                blockHeight = 0.05
            }
            ground -= blockHeight
        }
        return 0.0
    }

    fun isOnGround(height: Double): Boolean {
        return mc.theWorld.getCollidingBoundingBoxes(
            mc.thePlayer,
            mc.thePlayer.entityBoundingBox.offset(0.0, -height, 0.0)
        ).isNotEmpty()
    }

    fun getBaseMoveSpeed(): Double {
        var baseSpeed = 0.2875
        if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
            baseSpeed *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed)
                .getAmplifier() + 1)
        }
        return baseSpeed
    }


    @JvmStatic
    fun getDirectioon(): Double {
        var rotationYaw = mc.thePlayer.rotationYaw
        if (mc.thePlayer.moveForward < 0f) rotationYaw += 180f
        var forward = 1f
        if (mc.thePlayer.moveForward < 0f) forward = -0.5f else if (mc.thePlayer.moveForward > 0f) forward = 0.5f
        if (mc.thePlayer.moveStrafing > 0f) rotationYaw -= 90f * forward
        if (mc.thePlayer.moveStrafing < 0f) rotationYaw += 90f * forward
        return Math.toRadians(rotationYaw.toDouble())
    }

    fun isBlockUnder(): Boolean {
        if (mc.thePlayer == null) return false
        if (mc.thePlayer.posY < 0.0) {
            return false
        }
        var off = 0
        while (off < mc.thePlayer.posY.toInt() + 2) {
            val bb = mc.thePlayer.entityBoundingBox.offset(0.0, (-off).toDouble(), 0.0)
            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isNotEmpty()) {
                return true
            }
            off += 2
        }
        return false
    }
    fun setMotion2(d: Double, f: Float) {
        mc.thePlayer.motionX = -sin(Math.toRadians(f.toDouble())) * d
        mc.thePlayer.motionZ = cos(Math.toRadians(f.toDouble())) * d
    }
}
