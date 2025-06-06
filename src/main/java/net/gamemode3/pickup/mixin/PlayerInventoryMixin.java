package net.gamemode3.pickup.mixin;

import net.gamemode3.pickup.config.ModConfig;
import net.gamemode3.pickup.inventory.ContainerHelper;
import net.gamemode3.pickup.inventory.PlayerInventoryHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SetPlayerInventoryS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {
    @Shadow
    @Final
    public PlayerEntity player;

    @Shadow
    protected abstract int addStack(ItemStack stack);

    @Shadow
    public abstract int getEmptySlot();

    @Shadow
    @Final
    private DefaultedList<ItemStack> main;

    @Shadow
    public abstract void setStack(int slot, ItemStack stack);

    @Shadow
    public abstract ItemStack getStack(int slot);

    @Shadow public abstract int getSelectedSlot();

    @Shadow public abstract ItemStack getSelectedStack();

    @Shadow public abstract SetPlayerInventoryS2CPacket createSlotSetPacket(int slot);

    @Inject(method = "insertStack(ILnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void insertStack(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        System.out.println(stack);
        if (stack.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        try {
            System.out.println("Inserting stack");
            int initialStackCount = stack.getCount();
            if (slot != -1) {
                System.out.println("Inserting into specific slot: " + slot);
                Thread.dumpStack();
                ItemStack slotStack = this.getStack(slot);
                if (slotStack.isEmpty()) {
                    this.setStack(slot, stack.copy());
                    stack.setCount(0);
                    cir.setReturnValue(true);
                    return;
                }
                if (!ItemStack.areItemsAndComponentsEqual(slotStack, stack)) {
                    cir.setReturnValue(false);
                    return;
                }
                int freeSpace = slotStack.getMaxCount() - slotStack.getCount();
                int amountToAdd = Math.min(freeSpace, stack.getCount());
                slotStack.increment(amountToAdd);
                stack.decrement(amountToAdd);
                cir.setReturnValue(stack.getCount() < initialStackCount);
                return;
            }

            boolean stackChanged = true;
            while (!stack.isEmpty() && stackChanged) {
                System.out.println("Try adding remaining stack: " + stack);
                int previousCount = stack.getCount();
                stack.setCount(this.addStack(stack));
                stackChanged = stack.getCount() < previousCount;
            }
            System.out.println("Cannot add more items, stack is now: " + stack);

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

    @Inject(method = "addStack(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true)
    private void addStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        Pair<Integer, Integer> result = addStackGetSlot(stack);
        cir.setReturnValue(result.getLeft());
    }

    /**
     *
     * @param stack
     * The stack to add to the inventory.
     * @return
     * A pair where the first element is the remaining stack count
     * and the second element is the slot index where the stack was added.
     * If the slot index is -1, it means the stack was added to the off-hand.
     */
    @Unique
    private Pair<Integer, Integer> addStackGetSlot(ItemStack stack) {
        System.out.println("Adding stack: " + stack);
        Optional<Integer> stackingInfo;
        if (ModConfig.getAlwaysStackIntoHandContainer()) {
            stackingInfo = tryStackIntoHandContainer(stack);
            if (stackingInfo.isPresent()) {
                return new Pair<>(stack.getCount(), stackingInfo.get());
            }
        }
        if (ModConfig.getAlwaysPickUpIntoHandContainer()) {
            stackingInfo = tryPickUpIntoHandContainer(stack);
            if (stackingInfo.isPresent()) {
                return new Pair<>(stack.getCount(), stackingInfo.get());
            }
        }

        stackingInfo = tryStackIntoInventory(stack);
        if (stackingInfo.isPresent()) {
            return new Pair<>(stack.getCount(), stackingInfo.get());
        }

        if (ModConfig.getStackIntoContainers()) {
            stackingInfo = tryStackIntoContainer(stack);
            if (stackingInfo.isPresent()) {
                return new Pair<>(stack.getCount(), stackingInfo.get());
            }
        }

        stackingInfo = tryFillEmptySlot(stack);
        if (stackingInfo.isPresent()) {
            return new Pair<>(stack.getCount(), stackingInfo.get());
        }

        if (ModConfig.getPickUpIntoEmptyContainerSlots()) {
            stackingInfo = tryFillEmptyContainerSlot(stack);
            if (stackingInfo.isPresent()) {
                return new Pair<>(stack.getCount(), stackingInfo.get());
            }
        }

        return new Pair<>(stack.getCount(), -2); // No slots available
    }

    @Inject(method="offer", at = @At("HEAD"), cancellable = true)
    private void offer(ItemStack stack, boolean notifiesClient, CallbackInfo ci) {
        boolean stackChanged = true;
        while (!stack.isEmpty() && stackChanged) {
            System.out.println("Try adding remaining stack: " + stack);
            int previousCount = stack.getCount();
            Pair<Integer, Integer> result = this.addStackGetSlot(stack);

            stack.setCount(result.getLeft());
            stackChanged = stack.getCount() < previousCount;
            if (stackChanged && notifiesClient && this.player instanceof ServerPlayerEntity serverPlayerEntity) {
                int slot = result.getRight();
                if (slot >= 0) {
                    serverPlayerEntity.networkHandler.sendPacket(this.createSlotSetPacket(slot));
                }
                else if (slot == -1) {
                    serverPlayerEntity.networkHandler.sendPacket(PlayerInventoryHelper.createOffhandSetPacket(this.player));
                } else {
                    // slot == -2, no slots available
                    System.out.println("No slots available for stack: " + stack);
                }
            }
        }
        System.out.println("Cannot add more items, stack is now: " + stack);

        if (!stackChanged && this.player.isInCreativeMode()) {
            stack.setCount(0);
            return;
        }
        ci.cancel();
    }

    @Unique
    private Optional<Integer> tryStackIntoHandContainer(ItemStack stack) {
        ItemStack mainHandStack = this.getSelectedStack();
        if (ContainerHelper.tryStackIntoContainer(stack, mainHandStack)) {
            int mainHandSlot = this.getSelectedSlot();
            return Optional.of(mainHandSlot);
        }
        ItemStack offHandStack = this.player.getOffHandStack();
        if (ContainerHelper.tryStackIntoContainer(stack, offHandStack)) {
            return Optional.of(-1);
        }
        return Optional.empty();
    }

    @Unique
    private Optional<Integer> tryPickUpIntoHandContainer(ItemStack stack) {
        ItemStack mainHandStack = this.getSelectedStack();
        if (ContainerHelper.tryFillEmptyContainerSlot(stack, mainHandStack)) {
            int mainHandSlot = this.getSelectedSlot();
            return Optional.of(mainHandSlot);
        }

        ItemStack offHandStack = this.player.getOffHandStack();
        if (ContainerHelper.tryFillEmptyContainerSlot(stack, offHandStack)) {
            return Optional.of( -1);
        }
        return Optional.empty();
    }

    @Unique
    private Optional<Integer> tryFillEmptyContainerSlot(ItemStack stack) {
        ItemStack mainHandStack = this.getSelectedStack();
        if (ContainerHelper.tryFillEmptyContainerSlot(stack, mainHandStack)) {
            return Optional.of(this.getSelectedSlot());
        }

        ItemStack offHandStack = this.player.getOffHandStack();
        if (ContainerHelper.tryFillEmptyContainerSlot(stack, offHandStack)) return Optional.of(-1);

        for (int i = 0; i < this.main.size(); i++) {
            ItemStack containerStack = this.main.get(i);
            if (ContainerHelper.tryFillEmptyContainerSlot(stack, containerStack)) return Optional.of(i);
        }

        return Optional.empty();
    }

    @Unique
    private Optional<Integer> tryStackIntoInventory(ItemStack stack) {
        ItemStack selectedStack = this.getSelectedStack();
        if (addStackToOther(stack, selectedStack)) {
            return Optional.of(this.getSelectedSlot());
        }
        ItemStack offHandStack = this.player.getOffHandStack();
        if (addStackToOther(stack, offHandStack)) {
            return Optional.of(-1);
        }

        for (int i = 0; i < this.main.size(); i++) {
            ItemStack existingStack = this.main.get(i);
            if (!addStackToOther(stack, existingStack)) continue;
            this.main.set(i, existingStack);
            return Optional.of(i);
        }
        return Optional.empty();
    }

    @Unique
    private static boolean addStackToOther(ItemStack stack, ItemStack existingStack) {
        if (existingStack.isEmpty()) return false;

        if (!existingStack.isOf(stack.getItem())) {
            return false;
        }
        int freeSpace = existingStack.getMaxCount() - existingStack.getCount();
        if (freeSpace <= 0) {
            return false;
        }
        int amountToAdd = Math.min(freeSpace, stack.getCount());
        existingStack.increment(amountToAdd);
        stack.decrement(amountToAdd);
        return true;
    }

    @Unique
    private Optional<Integer> tryFillEmptySlot(ItemStack stack) {
        System.out.println("Trying to fill empty slot with: " + stack);
        int emptySlot = this.getEmptySlot();
        if (emptySlot != -1) {
            this.setStack(emptySlot, stack.copy());
            stack.setCount(0);
            return Optional.of(emptySlot);
        }
        return Optional.empty();
    }

    @Unique
    private Optional<Integer> tryStackIntoContainer(ItemStack stack) {
        ItemStack mainHandStack = this.getSelectedStack();
        if (ContainerHelper.tryStackIntoContainer(stack, mainHandStack)) {
            return Optional.of(this.getSelectedSlot());
        }

        ItemStack offHandStack = this.player.getOffHandStack();
        if (ContainerHelper.tryStackIntoContainer(stack, offHandStack)) {
            return Optional.of(-1);
        }

        for (int i = 0; i < this.main.size(); i++) {
            ItemStack containerStack = this.main.get(i);
            if (ContainerHelper.tryStackIntoContainer(stack, containerStack)) {
                return Optional.of(i);
            }
        }

        return Optional.empty();
    }
}
