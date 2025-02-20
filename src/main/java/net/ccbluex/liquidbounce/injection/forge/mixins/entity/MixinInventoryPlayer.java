/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.utils.SilentHotbar;
import net.minecraft.entity.player.InventoryPlayer;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;

@Mixin(InventoryPlayer.class)
public class MixinInventoryPlayer {

    @Redirect(method = {"getCurrentItem", "decrementAnimations", "getStrVsBlock", "canHeldItemHarvest"}, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/InventoryPlayer;currentItem:I", opcode = Opcodes.GETFIELD))
    private int hookSilentHotbar(InventoryPlayer instance) {
        return instance.player.getGameProfile() == mc.thePlayer.getGameProfile() ? SilentHotbar.INSTANCE.getCurrentSlot() : instance.currentItem;
    }
}
