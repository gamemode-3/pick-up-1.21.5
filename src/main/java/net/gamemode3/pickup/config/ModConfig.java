package net.gamemode3.pickup.config;

public class ModConfig {
    public static boolean getStackIntoContainers() {
        return true;
    }

    public static boolean getBundleEmptySlotEnabled() {
        return false;
    }

    public static boolean getShulkerBoxEmptySlotEnabled() {
        return true;
    }

    public static boolean getPickUpIntoEmptyContainerSlots() {
        return getBundleEmptySlotEnabled() || getShulkerBoxEmptySlotEnabled();
    }

    public static boolean getBundleEnabled() {
        return true;
    }

    public static boolean getShulkerBoxEnabled() {
        return true;
    }

    public static boolean getAlwaysStackIntoOffhandContainer() {
        return true;
    }

    public static boolean getAlwaysPickUpIntoOffhandContainer() {
        return  true;
    }

    public static boolean getPickFromShulkerBoxEnabled() { return true; }

    public static boolean getPickFromBundleEnabled() { return true; }
}
