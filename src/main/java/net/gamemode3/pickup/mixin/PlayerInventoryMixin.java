package net.gamemode3.pickup.mixin;

import net.gamemode3.pickup.config.ModConfig;
import net.gamemode3.pickup.inventory.ContainerHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

        if (ModConfig.getStackIntoContainers() && this.tryStackIntoContainer(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }

        if (tryFillEmptySlot(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }

        if (ModConfig.getPickUpIntoEmptyContainerSlots() && this.tryFillEmptyContainerSlot(stack)) {
            cir.setReturnValue(stack.getCount());
            return;
        }

        cir.setReturnValue(stack.getCount());
    }

    @Unique
    private boolean tryStackIntoOffHandContainer(ItemStack stack) {
        ItemStack offHandStack = this.player.getOffHandStack();
        return ContainerHelper.tryStackIntoContainer(stack, offHandStack);
    }

    @Unique
    private boolean tryPickUpIntoOffHandContainer(ItemStack stack) {
        System.out.println("Trying to pick up into offhand container: " + stack);
        ItemStack offHandStack = this.player.getOffHandStack();
        return ContainerHelper.tryFillEmptyContainerSlot(stack, offHandStack, false);
    }

    @Unique
    private boolean tryFillEmptyContainerSlot(ItemStack stack) {
        ItemStack offHandStack = this.player.getOffHandStack();
        if (ContainerHelper.tryFillEmptyContainerSlot(stack, offHandStack)) return true;

        for (ItemStack containerStack : this.main) {
            if (ContainerHelper.tryFillEmptyContainerSlot(stack, containerStack)) return true;
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

    @Unique
    private boolean tryStackIntoContainer(ItemStack stack) {
        ItemStack offHandStack = this.player.getOffHandStack();
        if (ContainerHelper.tryStackIntoContainer(stack, offHandStack)) return true;

        for (ItemStack containerStack : this.main) {
            if (ContainerHelper.tryStackIntoContainer(stack, containerStack)) return true;
        }

        return false;
    }
}
