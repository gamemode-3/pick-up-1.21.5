package net.gamemode3.pickup.inventory;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShulkerBoxHelper {
    public static boolean tryAddIntoShulkerBox(ItemStack stack, ItemStack shulkerBoxStack) {
        if (ShulkerBoxHelper.tryStackIntoShulkerBox(stack, shulkerBoxStack)) {
            return true;
        }

        if (stack.isIn(ItemTags.SHULKER_BOXES)) {
            return false;
        }

        ContainerComponent containerComponent = shulkerBoxStack.get(
                DataComponentTypes.CONTAINER
        );
        if (containerComponent == null) {
            return false;
        }

        List<ItemStack> stacks = new ArrayList<>(containerComponent.stream().toList());
        if (stacks.size() < 27) {
            for (int i = stacks.size(); i < 27; i++) {
                stacks.add(ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack storedStack = stacks.get(i);
            if (!storedStack.isEmpty()) continue;

            stacks.set(i, stack.copy());
            stack.setCount(0);

            ContainerComponent newContainerComponent = ContainerComponent.fromStacks(stacks);
            shulkerBoxStack.set(DataComponentTypes.CONTAINER, newContainerComponent);
            return true;
        }
        return false;
    }

    public static boolean tryStackIntoShulkerBox(ItemStack stack, ItemStack shulkerBoxStack) {
        ContainerComponent containerComponent = shulkerBoxStack.get(
                DataComponentTypes.CONTAINER
        );
        if (containerComponent == null) {
            return false;
        }

        List<ItemStack> stacks = containerComponent.stream().toList();
        for (ItemStack storedStack : stacks) {
            if (storedStack.isEmpty()) continue;
            if (!storedStack.isOf(stack.getItem())) continue;

            int freeSpace = storedStack.getMaxCount() - storedStack.getCount();
            if (!(freeSpace > 0)) continue;

            int amountToAdd = Math.min(freeSpace, stack.getCount());
            storedStack.increment(amountToAdd);
            stack.decrement(amountToAdd);
            shulkerBoxStack.set(DataComponentTypes.CONTAINER, containerComponent);

            ContainerComponent newContainerComponent = ContainerComponent.fromStacks(stacks);
            shulkerBoxStack.set(DataComponentTypes.CONTAINER, newContainerComponent);
            return true;
        }
        return false;
    }

    public static Optional<Pair<Integer, ItemStack>> tryExtractFromShulkerBox(ItemStack stack, ItemStack shulkerBoxStack) {
        ContainerComponent containerComponent = shulkerBoxStack.get(
                DataComponentTypes.CONTAINER
        );
        if (containerComponent == null) {
            return Optional.empty();
        }

        List<ItemStack> stacks = new ArrayList<>(containerComponent.stream().toList());

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack storedStack = stacks.get(i);
            if (storedStack.isEmpty()) continue;
            if (!storedStack.isOf(stack.getItem())) continue;

            ItemStack removedStack = storedStack.copy();
            stacks.set(i, ItemStack.EMPTY);

            ContainerComponent newContainerComponent = ContainerComponent.fromStacks(stacks);
            shulkerBoxStack.set(DataComponentTypes.CONTAINER, newContainerComponent);

            return Optional.of(new Pair<>(i, removedStack));
        }
        return Optional.empty();
    }

    public static boolean tryPutIntoShulkerBox(ItemStack stack, ItemStack shulkerBoxStack, int slot) {
        ContainerComponent containerComponent = shulkerBoxStack.get(DataComponentTypes.CONTAINER);
        if (containerComponent == null) {
            return false;
        }

        List<ItemStack> stacks = new ArrayList<>(containerComponent.stream().toList());
        if (stacks.size() <= slot) {
            for (int i = stacks.size(); i <= slot; i++) {
                stacks.add(ItemStack.EMPTY);
            }
        }
        stacks.set(slot, stack.copy());
        shulkerBoxStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
        return true;
    }
}