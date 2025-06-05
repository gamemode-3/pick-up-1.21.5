package net.gamemode3.pickup.inventory;

import net.gamemode3.pickup.mixin.PlayerInventoryInvoker;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Pair;

import java.util.Optional;

public class PlayerInventoryHelper {
    public static Optional<Pair<Integer, Pair<Integer, ItemStack>>> tryExtractStackFromContainer(PlayerInventory inventory, ItemStack stack, boolean enableShulkerBox, boolean enableBundle) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack containerStack = inventory.getStack(i);
            if (enableShulkerBox && containerStack.isIn(ItemTags.SHULKER_BOXES)) {
                Optional<Pair<Integer, ItemStack>> extractedItemInfo = ShulkerBoxHelper.tryExtractFromShulkerBox(stack, containerStack);
                if (extractedItemInfo.isPresent()) {
                    Pair<Integer, ItemStack> extractedItem = extractedItemInfo.get();
                    return Optional.of(new Pair<>(i, extractedItem));
                }
            }
            else if (enableBundle && containerStack.isIn(ItemTags.BUNDLES)) {
                Optional<Pair<Integer, ItemStack>> extractedItemInfo = BundleHelper.tryExtractFromBundle(stack, containerStack);
                if (extractedItemInfo.isPresent()) {
                    Pair<Integer, ItemStack> extractedItem = extractedItemInfo.get();
                    return Optional.of(new Pair<>(i, extractedItem));
                }
            }
        }
        return Optional.empty();
    }

    public static boolean tryFillEmptySlot(PlayerInventory inventory, ItemStack stack) {
        int emptySlot = inventory.getEmptySlot();
        if (emptySlot != -1) {
            stack.setCount(((PlayerInventoryInvoker) inventory).invokeAddStack(emptySlot, stack));
            return stack.getCount() < stack.getMaxCount();
        }
        return false;
    }

    public static Pair<Integer, ItemStack> findHotbarStackToReplace(PlayerInventory playerInventory) {
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarStack = playerInventory.getStack(i);
            if (hotbarStack.isEmpty()) {
                return new Pair<>(i, hotbarStack);
            }
        }
        int selectedSlot = playerInventory.getSelectedSlot();
        ItemStack selectedStack = playerInventory.getStack(selectedSlot);
        return new Pair<>(selectedSlot, selectedStack);
    }

    public static boolean tryPutIntoContainer(PlayerInventory playerInventory, int containerSlotInInventory, int itemSlotInContainer, ItemStack stack) {
        ItemStack containerStack = playerInventory.getStack(containerSlotInInventory);
        if (containerStack.isIn(ItemTags.SHULKER_BOXES)) {
            return ShulkerBoxHelper.tryPutIntoShulkerBox(stack, containerStack, itemSlotInContainer);
        }
        if (containerStack.isIn(ItemTags.BUNDLES)) {
            return BundleHelper.tryAddIntoBundle(stack, containerStack);
        }
        return false;
    }
}
