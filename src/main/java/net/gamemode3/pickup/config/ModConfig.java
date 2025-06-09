package net.gamemode3.pickup.config;

import com.mojang.datafixers.util.Pair;
import net.gamemode3.pickup.PickUp;

public class ModConfig {
    private static SimpleConfig CONFIG;
    private static ModConfigProvider configs;

    private static boolean STACK_INTO_SHULKER_BOX;
    private static boolean STACK_INTO_BUNDLE;

    private static boolean PICK_UP_INTO_SHULKER_BOX;
    private static boolean PICK_UP_INTO_BUNDLE;

    private static boolean ALWAYS_STACK_INTO_EQUIPPED_SHULKER_BOX;
    private static boolean ALWAYS_STACK_INTO_EQUIPPED_BUNDLE;
    private static boolean ALWAYS_PICK_UP_INTO_EQUIPPED_SHULKER_BOX;
    private static boolean ALWAYS_PICK_UP_INTO_EQUIPPED_BUNDLE;

    private static boolean PICK_FROM_SHULKER_BOX;
    private static boolean PICK_FROM_BUNDLE;

    public static void registerConfigs() {
        configs = new ModConfigProvider();
        createConfigs();

        CONFIG = SimpleConfig.of(PickUp.MOD_ID + "-config").provider(configs).request();

        assignConfigs();
    }

    private static void createConfigs() {
        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.stack-into-shulker-box", true),
                "when picking up items, stack them onto unfinished stacks in shulker boxes in the player's inventory.\n"
                        + "unfinished stacks in the inventory will be prioritized."
        );
        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.stack-into-bundle", true),
                "when picking up items, stack them onto unfinished stacks in bundles in the player's inventory.\n"
                + "unfinished stacks in the inventory will be prioritized."
        );

        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.pick-up-into-shulker-box", true),
                "when picking up items, allow adding them into empty shulker box slots.\n"
                        + "empty space in the inventory will be prioritized."
        );
        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.pick-up-into-bundle", false),
                "when picking up items, allow adding them into bundles even when the bundle is not holding any items of the same type.\n"
                        + "empty space in the inventory will be prioritized."
        );

        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.always-stack-into-equipped-shulker-box", true),
                "when picking up items while holding a shulker box with unfinished stacks, prioritize stacking onto them."
        );
        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.always-stack-into-equipped-bundle", true),
                "when picking up items while holding a bundle with unfinished stacks, prioritize stacking onto them."
        );

        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.always-pick-up-into-equipped-shulker-box", true),
                "when picking up items while holding a shulker box with space for them, always pick up into that container."
        );
        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.always-pick-up-into-equipped-bundle", true),
                "when picking up items while holding a bundle with space for them, always pick up into that container."
        );

        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.pick-from-shulker-box", true),
                "when using pick block/entity (middle mouse button), allow picking the item from shulker boxes in the inventory.\n"
                + "if the item is in the inventory itself, that will be prioritized."
        );
        configs.addKeyValuePair(
                new Pair<>("pick-up.inventory-containers.pick-from-bundle", true),
                "when using pick block/entity (middle mouse button), allow picking the item from bundles in the inventory.\n"
                        + "if the item is in the inventory itself, that will be prioritized."
        );
    }

    private static void assignConfigs() {
        STACK_INTO_SHULKER_BOX = CONFIG.getOrDefault("pick-up.inventory-containers.stack-into-shulker-box", true);
        STACK_INTO_BUNDLE = CONFIG.getOrDefault("pick-up.inventory-containers.stack-into-bundle", true);

        PICK_UP_INTO_SHULKER_BOX = CONFIG.getOrDefault("pick-up.inventory-containers.pick-up-into-shulker-box", true);
        PICK_UP_INTO_BUNDLE = CONFIG.getOrDefault("pick-up.inventory-containers.pick-up-into-bundle", false);

        ALWAYS_STACK_INTO_EQUIPPED_SHULKER_BOX = CONFIG.getOrDefault("pick-up.inventory-containers.always-stack-into-equipped-shulker-box", true);
        ALWAYS_STACK_INTO_EQUIPPED_BUNDLE = CONFIG.getOrDefault("pick-up.inventory-containers.always-stack-into-equipped-bundle", true);

        ALWAYS_PICK_UP_INTO_EQUIPPED_SHULKER_BOX = CONFIG.getOrDefault("pick-up.inventory-containers.always-pick-up-into-equipped-shulker-box", true);
        ALWAYS_PICK_UP_INTO_EQUIPPED_BUNDLE = CONFIG.getOrDefault("pick-up.inventory-containers.always-pick-up-into-equipped-bundle", true);

        PICK_FROM_SHULKER_BOX = CONFIG.getOrDefault("pick-up.inventory-containers.pick-from-shulker-box", true);
        PICK_FROM_BUNDLE = CONFIG.getOrDefault("pick-up.inventory-containers.pick-from-bundle", true);


        PickUp.LOGGER.info("all {} configs for {} have been set properly", configs.size(), PickUp.MOD_ID);
    }

    public static boolean getStackIntoContainers() {
        return getStackIntoBundle() || getStackIntoShulkerBox();
    }

    public static boolean getStackIntoBundle() {
        return STACK_INTO_BUNDLE;
    }

    public static boolean getStackIntoShulkerBox() {
        return STACK_INTO_SHULKER_BOX;
    }

    public static boolean getPickUpIntoBundle() {
        return PICK_UP_INTO_BUNDLE;
    }

    public static boolean getPickUpIntoShulkerBox() {
        return PICK_UP_INTO_SHULKER_BOX;
    }

    public static boolean getPickUpIntoContainers() {
        return getPickUpIntoBundle() || getPickUpIntoShulkerBox();
    }

    public static boolean getAlwaysStackIntoEquippedShulkerBox() {
        return ALWAYS_STACK_INTO_EQUIPPED_SHULKER_BOX;
    }

    public static boolean getAlwaysStackIntoEquippedBundle() {
        return ALWAYS_STACK_INTO_EQUIPPED_BUNDLE;
    }

    public static boolean getAlwaysStackIntoEquippedContainer() {
        return getAlwaysStackIntoEquippedBundle() || getAlwaysStackIntoEquippedShulkerBox();
    }

    public static boolean getAlwaysPickUpIntoEquippedShulkerBox() {
        return ALWAYS_PICK_UP_INTO_EQUIPPED_SHULKER_BOX;
    }

    public static boolean getAlwaysPickUpIntoEquippedBundle() {
        return ALWAYS_PICK_UP_INTO_EQUIPPED_BUNDLE;
    }

    public static boolean getAlwaysPickUpIntoEquippedContainer() {
        return getAlwaysPickUpIntoEquippedBundle() || getAlwaysPickUpIntoEquippedShulkerBox();
    }

    public static boolean getPickFromShulkerBox() {
        return PICK_FROM_SHULKER_BOX;
    }

    public static boolean getPickFromBundle() {
        return PICK_FROM_BUNDLE;
    }
}
