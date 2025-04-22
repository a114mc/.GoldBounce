package net.ccbluex.liquidbounce.features.module.modules.settings

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.value.BoolValue

object Debugger : Module("Debugger", Category.SETTINGS, gameDetecting = false, hideModule = false){
    val RotationDebug by BoolValue("Rotation", false)
}