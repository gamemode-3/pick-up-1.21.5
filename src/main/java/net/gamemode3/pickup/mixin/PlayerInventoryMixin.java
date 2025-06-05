package net.gamemode3.pickup.mixin;

import net.gamemode3.pickup.config.ModConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    @Shadow @Final public PlayerEntity player;

    @Shadow protected abstract int addStack(ItemStack stack);

    @Shadow protected abstract int addStack(int slot, ItemStack stack);

    @Shadow public abstract int getEmptySlot();

    @Shadow @Final private DefaultedList<ItemStack> main;

    @Inject(method="insertStack(ILnet/minecraft/item/ItemStack;)Z", at=@At("HEAD"), cancellable = true)
    private void insertStack(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        try {
            int initialStackCount = stack.getCount();
            if (slot != -1) {
                stack.setCount(this.addStack(slot, stack));
                cir.setReturnValue(stack.getCount() < initialStackCount);
                return;
            }

            boolean stackChanged = true;
            while (!stack.isEmpty() && stackChanged) {
                int previousCount = stack.getCount();
                stack.setCount(this.addStack(stack));
                stackChanged = stack.getCount() < previousCount;
            }

            if (!stackChanged && this.player.isInCreativeMode()) {
                stack.setCount(0);
                cir.setReturnValue(true);
                return;
            }
            cir.setReturnValue(stackChanged);
        } catch (Throwable e) {
            CrashReport crashReport = CrashReport.create(e, "Adding item to inventory");
            CrashReportSection crashReportSection = crashReport.addElement("Item being added");
            crashReportSection.add("Item ID", Item.getRawId(stack.getItem()));
            crashReportSection.add("Item data", stack.getDamage());
            crashReportSection.add("Item name", (() -> stack.getName().getString()));
            throw new CrashException(crashReport);
        }
    }

    @Inject(method="addStack(Lnet/minecraft/item/ItemStack;)I", at=@At("HEAD"), cancellable = true)
    private void addStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.getAlwaysStackIntoOffhandContainer() && tryStackIntoOffHandContainer(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }
        if (ModConfig.getAlwaysPickUpIntoOffhandContainer() && tryPickUpIntoOffHandContainer(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }

        if (tryStackIntoInventory(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }

        if (ModConfig.getStackIntoContainers() && tryStackIntoContainer(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }

        if (tryFillEmptySlot(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }

        if (ModConfig.getPickUpIntoEmptyContainerSlots() && tryFillEmptyContainerSlot(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }

        cir.setReturnValue(stack.getCount());
    }

    @Unique
    private boolean tryStackIntoOffHandContainer(ItemStack stack) {
        ItemStack offHandStack = this.player.getOffHandStack();
        return tryStackIntoContainer(stack, offHandStack);
    }

    @Unique
    private boolean tryPickUpIntoOffHandContainer(ItemStack stack) {
        System.out.println("Trying to pick up into offhand container: " + stack);
        ItemStack offHandStack = this.player.getOffHandStack();
        return tryFillEmptyContainerSlot(stack, offHandStack, false);
    }

    @Unique
    private boolean tryFillEmptyContainerSlot(ItemStack stack) {
        ItemStack offHandStack = this.player.getOffHandStack();
        if (tryFillEmptyContainerSlot(stack, offHandStack)) return true;

        for (ItemStack containerStack : this.main) {
            if (tryFillEmptyContainerSlot(stack, containerStack)) return true;
        }

        return false;
    }

    @Unique
    private boolean tryFillEmptyContainerSlot(ItemStack stack, ItemStack containerStack) {
        return tryFillEmptyContainerSlot(stack, containerStack, true);
    }

    @Unique
    private boolean tryFillEmptyContainerSlot(ItemStack stack, ItemStack containerStack, boolean requireSpecificEmptySlotSetting) {
        System.out.println("Trying to pick up into empty container: " + containerStack);
        if (containerStack.isEmpty()) return false;
        System.out.println("Slot is not empty");

        if (containerStack.isOf(Items.BUNDLE) && ModConfig.getBundleEnabled()) {
            if (requireSpecificEmptySlotSetting && !ModConfig.getBundleEmptySlotEnabled()) return false;
            return tryAddIntoBundle(stack, containerStack);
        }
        if (containerStack.isOf(Items.SHULKER_BOX) && ModConfig.getShulkerBoxEnabled()) {
            if (requireSpecificEmptySlotSetting && !ModConfig.getShulkerBoxEmptySlotEnabled()) return false;
            System.out.println("Item is a shulker box");
            return tryFillEmptyShulkerBoxSlot(stack, containerStack);
        }
        return false;
    }

    @Unique
    private boolean tryAddIntoBundle(ItemStack stack, ItemStack bundleStack) {
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

    @Unique
    private boolean tryFillEmptyShulkerBoxSlot(ItemStack stack, ItemStack shulkerBoxStack) {
        // get the block entity of the shulker box
        ContainerComponent containerComponent = shulkerBoxStack.get(
                DataComponentTypes.CONTAINER
        );
        if (containerComponent == null) {
            return false; // No block entity data, cannot stack into shulker box
        }

        List<ItemStack> stacks = new ArrayList<>(containerComponent.stream().toList());
        if (stacks.size() < 27) {
            // If the shulker box has less than 27 stacks, fill it up to 27
            for (int i = stacks.size(); i < 27; i++) {
                stacks.add(ItemStack.EMPTY);
            }
        }
        for (int i = 0; i < stacks.size(); i++) { // for whatever reason i cannot use a for-each loop here, it just won't let me!!!!
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

    @Unique
    private boolean tryStackIntoInventory(ItemStack stack) {
        for (int i = 0; i < this.main.size(); i++) {
            ItemStack existingStack = this.main.get(i);
            if (existingStack.isEmpty()) continue;

            if (!existingStack.isOf(stack.getItem())) {
                continue; // Skip if the item is not the same
            }
            int freeSpace = existingStack.getMaxCount() - existingStack.getCount();
            if (freeSpace <= 0) {
                continue; // No free space in this stack
            }
            int amountToAdd = Math.min(freeSpace, stack.getCount());
            existingStack.increment(amountToAdd);
            stack.decrement(amountToAdd);
            this.main.set(i, existingStack);
            return true;
        }
        return false;
    }

    @Unique
    private boolean tryFillEmptySlot(ItemStack stack) {
        int emptySlot = this.getEmptySlot();
        if (emptySlot != -1) {
            stack.setCount(this.addStack(emptySlot, stack));
            return stack.getCount() < stack.getMaxCount();
        }
        return false;
    }

    /**
     * Returns a pair of integers representing the slot the container is in
     * and the slot within the container that has room for the stack.
     * Returns Pair(-1, -1) if no such slot is found.
     */
    @Unique
    private boolean tryStackIntoContainer(ItemStack stack) {
        ItemStack offHandStack = this.player.getOffHandStack();
        if (tryStackIntoContainer(stack, offHandStack)) return true;

        for (ItemStack containerStack : this.main) {
            if (tryStackIntoContainer(stack, containerStack)) return true;
        }

        return false;
    }

    @Unique
    private boolean tryStackIntoContainer(ItemStack stack, ItemStack containerStack) {
        if (containerStack.isEmpty()) return false;

        if (containerStack.isOf(Items.BUNDLE) && ModConfig.getBundleEnabled()) {
            return tryStackIntoBundle(stack, containerStack);
        }
        if (containerStack.isOf(Items.SHULKER_BOX) && ModConfig.getShulkerBoxEnabled()) {
            return tryStackIntoShulkerBox(stack, containerStack);
        }
        return false;
    }

    @Unique
    private boolean tryStackIntoBundle(ItemStack stack, ItemStack bundleStack) {
        // check if the bundle contains a stack of the same item
        boolean hasSameItem = false;
        int selectedSlot = BundleItem.getSelectedStackIndex(bundleStack);
        for (int j = 0; j < BundleItem.getNumberOfStacksShown(bundleStack); j++) {
            BundleItem.setSelectedStackIndex(bundleStack, j);
            ItemStack bundleContent = BundleItem.getSelectedStack(bundleStack);
            if (bundleContent.isEmpty()) continue;
            if (bundleContent.isOf(stack.getItem())) {
                hasSameItem = true;
                break;
            }
        }
        // make sure we don't mess nothin' up innit
        BundleItem.setSelectedStackIndex(bundleStack, selectedSlot);

        if (hasSameItem) {
            return tryAddIntoBundle(stack, bundleStack);
        }
        return false;
    }

    @Unique
    private boolean tryStackIntoShulkerBox(ItemStack stack, ItemStack shulkerBoxStack) {
        // get the block entity of the shulker box
        ContainerComponent containerComponent = shulkerBoxStack.get(
                DataComponentTypes.CONTAINER
        );
        if (containerComponent == null) {
            return false; // No block entity data, cannot stack into shulker box
        }

        List<ItemStack> stacks = containerComponent.stream().toList();
        for (ItemStack storedStack : stacks) { // for whatever reason i cannot use a for-each loop here, it just won't let me!!!!
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
}
