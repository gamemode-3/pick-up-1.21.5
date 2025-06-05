package net.gamemode3.pickup.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerInventory.class)
public interface PlayerInventoryInvoker {
    @Invoker("addStack")
    int invokeAddStack(int slot, ItemStack stack);
}
