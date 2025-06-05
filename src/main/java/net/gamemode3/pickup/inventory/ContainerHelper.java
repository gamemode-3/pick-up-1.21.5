package net.gamemode3.pickup.inventory;

import net.gamemode3.pickup.config.ModConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

import static net.gamemode3.pickup.inventory.BundleHelper.tryAddIntoBundle;
import static net.gamemode3.pickup.inventory.BundleHelper.tryStackIntoBundle;
import static net.gamemode3.pickup.inventory.ShulkerBoxHelper.tryFillEmptyShulkerBoxSlot;
import static net.gamemode3.pickup.inventory.ShulkerBoxHelper.tryStackIntoShulkerBox;

public class ContainerHelper {

    public static boolean tryStackIntoContainer(ItemStack stack, ItemStack containerStack) {
        if (containerStack.isEmpty()) return false;

        if (containerStack.isIn(ItemTags.BUNDLES) && ModConfig.getBundleEnabled()) {
            return tryStackIntoBundle(stack, containerStack);
        }
        if (containerStack.isIn(ItemTags.SHULKER_BOXES) && ModConfig.getShulkerBoxEnabled()) {
            return tryStackIntoShulkerBox(stack, containerStack);
        }
        return false;
    }

    public static boolean tryFillEmptyContainerSlot(ItemStack stack, ItemStack containerStack) {
        return tryFillEmptyContainerSlot(stack, containerStack, true);
    }

    public static boolean tryFillEmptyContainerSlot(ItemStack stack, ItemStack containerStack, boolean requireSpecificEmptySlotSetting) {
        System.out.println("Trying to pick up into empty container: " + containerStack);
        if (containerStack.isEmpty()) return false;
        System.out.println("Slot is not empty");

        if (containerStack.isIn(ItemTags.BUNDLES) && ModConfig.getBundleEnabled()) {
            if (requireSpecificEmptySlotSetting && !ModConfig.getBundleEmptySlotEnabled()) return false;
            return tryAddIntoBundle(stack, containerStack);
        }
        if (containerStack.isIn(ItemTags.SHULKER_BOXES) && ModConfig.getShulkerBoxEnabled()) {
            if (requireSpecificEmptySlotSetting && !ModConfig.getShulkerBoxEmptySlotEnabled()) return false;
            System.out.println("Item is a shulker box");
            return tryFillEmptyShulkerBoxSlot(stack, containerStack);
        }
        return false;
    }
}
