package net.gamemode3.pickup.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

import static net.gamemode3.pickup.inventory.BundleHelper.tryAddIntoBundle;
import static net.gamemode3.pickup.inventory.BundleHelper.tryStackIntoBundle;
import static net.gamemode3.pickup.inventory.ShulkerBoxHelper.tryAddIntoShulkerBox;
import static net.gamemode3.pickup.inventory.ShulkerBoxHelper.tryStackIntoShulkerBox;

public class ContainerHelper {

    public static boolean tryStackIntoContainer(ItemStack stack, ItemStack containerStack, boolean useShulkerBoxes, boolean useBundles) {
        if (containerStack.isEmpty()) return false;

        if (containerStack.isIn(ItemTags.SHULKER_BOXES) && useShulkerBoxes) {
            return tryStackIntoShulkerBox(stack, containerStack);
        }
        if (containerStack.isIn(ItemTags.BUNDLES) && useBundles) {
            return tryStackIntoBundle(stack, containerStack);
        }
        return false;
    }

//    public static boolean tryPickUpIntoContainer(ItemStack stack, ItemStack containerStack) {
//        return tryPickUpIntoContainer(stack, containerStack, ModConfig.getPickUpIntoShulkerBox(), ModConfig.getPickUpIntoBundle());
//    }

    public static boolean tryPickUpIntoContainer(ItemStack stack, ItemStack containerStack, boolean useShulkerBoxes, boolean useBundles) {
        if (containerStack.isEmpty()) return false;

        if (containerStack.isIn(ItemTags.SHULKER_BOXES) && useShulkerBoxes) {
            return tryAddIntoShulkerBox(stack, containerStack);
        }
        if (containerStack.isIn(ItemTags.BUNDLES) && useBundles) {
            return tryAddIntoBundle(stack, containerStack);
        }
        return false;
    }
}
