package net.gamemode3.pickup.mixin;

import net.gamemode3.pickup.config.ModConfig;
import net.gamemode3.pickup.inventory.PlayerInventoryHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
    @Shadow public ServerPlayerEntity player;

    @Inject(method="onPickItem", at=@At("HEAD"), cancellable = true)
    private void onPickItem(ItemStack stack, CallbackInfo ci) {
        if (stack.isItemEnabled(this.player.getWorld().getEnabledFeatures())) {
            PlayerInventory playerInventory = this.player.getInventory();
            int i = playerInventory.getSlotWithStack(stack);
            if (i != -1) {
                if (PlayerInventory.isValidHotbarIndex(i)) {
                    playerInventory.setSelectedSlot(i);
                } else {
                    playerInventory.swapSlotWithHotbar(i);
                }
            } else {
                boolean enableShulkerBox = ModConfig.getPickFromShulkerBoxEnabled();
                boolean enableBundle = ModConfig.getPickFromBundleEnabled();
                Optional<Pair<Integer, Pair<Integer, ItemStack>>> extractedItemInfo = PlayerInventoryHelper.tryExtractStackFromContainer(playerInventory, stack, enableShulkerBox, enableBundle);

                if (extractedItemInfo.isPresent()) {
                    Pair<Integer, Pair<Integer, ItemStack>> extractedItem = extractedItemInfo.get();
                    int containerSlotInInventory = extractedItem.getLeft();
                    Pair<Integer, ItemStack> containerItem = extractedItem.getRight();
                    int itemSlotInContainer = containerItem.getLeft();
                    ItemStack extractedItemStack = containerItem.getRight().copy();

                    Pair<Integer, ItemStack> hotbarReplaceInfo = PlayerInventoryHelper.findHotbarStackToReplace(playerInventory);
                    Integer hotbarReplaceIndex = hotbarReplaceInfo.getLeft();
                    ItemStack previousHotbarStack = hotbarReplaceInfo.getRight();

                    if (previousHotbarStack.isEmpty()) {
                        playerInventory.setStack(hotbarReplaceIndex, extractedItemStack);
                        playerInventory.setSelectedSlot(hotbarReplaceIndex);
                    } else {
                        boolean success = PlayerInventoryHelper.tryFillEmptySlot(playerInventory, previousHotbarStack);
                        if (!success) {
                            success = PlayerInventoryHelper.tryPutIntoContainer(playerInventory, containerSlotInInventory, itemSlotInContainer, previousHotbarStack);
                        }
                        if (!success) {
                            if (!PlayerInventoryHelper.tryPutIntoContainer(playerInventory, containerSlotInInventory, itemSlotInContainer, extractedItemStack)) {
                                // Can't put the item back where it came from, that should not happen
                                throw new RuntimeException("Failed to put item back into container: " + extractedItemStack);
                            }
                            if (this.player.isInCreativeMode()) {
                                playerInventory.swapStackWithHotbar(stack);
                            }

                        }
                        else {
                            playerInventory.setStack(hotbarReplaceIndex, extractedItemStack);
                            playerInventory.setSelectedSlot(hotbarReplaceIndex);
                        }
                    }

                } else if (this.player.isInCreativeMode()) {
                    playerInventory.swapStackWithHotbar(stack);
                }
            }

            this.player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(playerInventory.getSelectedSlot()));
            this.player.playerScreenHandler.sendContentUpdates();
            ci.cancel();
        }
    }
}
