package net.gamemode3.pickup.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import java.util.Objects;
import java.util.Optional;

public class BundleHelper {
    public static boolean tryAddIntoBundle(ItemStack stack, ItemStack bundleStack) {
        System.out.println("Trying to pick up into bundle: " + bundleStack);
        BundleContentsComponent bundleContentsComponent = bundleStack.getOrDefault(
                DataComponentTypes.BUNDLE_CONTENTS,
                BundleContentsComponent.DEFAULT
        );
        BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
        int addedItems = builder.add(stack);

        bundleStack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
        //                this.onContentChanged(player);
        return addedItems > 0;
    }

    public static boolean tryStackIntoBundle(ItemStack stack, ItemStack bundleStack) {
        // check if the bundle contains a stack of the same item
        boolean hasSameItem = findItemInBundle(bundleStack, stack) != -1;

        if (hasSameItem) {
            return tryAddIntoBundle(stack, bundleStack);
        }
        return false;
    }

    public static int findItemInBundle(ItemStack bundleStack, ItemStack stack) {
        int selectedSlot = BundleItem.getSelectedStackIndex(bundleStack);
        for (int i = 0; i < BundleItem.getNumberOfStacksShown(bundleStack); i++) {
            BundleItem.setSelectedStackIndex(bundleStack, i);
            ItemStack bundleContent = BundleItem.getSelectedStack(bundleStack);
            if (bundleContent.isEmpty()) continue;
            if (ItemStack.areItemsAndComponentsEqual(bundleContent, stack)) {
                BundleItem.setSelectedStackIndex(bundleStack, selectedSlot);
                return i;
            }
        }
        // make sure we don't mess nothin' up innit
        BundleItem.setSelectedStackIndex(bundleStack, selectedSlot);
        return -1;
    }

    public static Optional<Pair<Integer, ItemStack>> tryExtractFromBundle(ItemStack stack, ItemStack bundleStack) {
        int stackIndex = findItemInBundle(bundleStack, stack);
        if (stackIndex == -1) {
            return Optional.empty(); // Item not found in bundle
        }
        int selectedSlot = BundleItem.getSelectedStackIndex(bundleStack);
        BundleItem.setSelectedStackIndex(bundleStack, stackIndex);
        ItemStack bundleContent = BundleItem.getSelectedStack(bundleStack);
        BundleContentsComponent bundleContentsComponent = Objects.requireNonNull(bundleStack.get(DataComponentTypes.BUNDLE_CONTENTS));
        BundleContentsComponent.Builder builder = new BundleContentsComponent.Builder(bundleContentsComponent);
        builder.removeSelected();
        bundleStack.set(DataComponentTypes.BUNDLE_CONTENTS, builder.build());
        if (selectedSlot > stackIndex) selectedSlot--;
        BundleItem.setSelectedStackIndex(bundleStack, selectedSlot);
        return Optional.of(new Pair<>(stackIndex, bundleContent));
    }
}
