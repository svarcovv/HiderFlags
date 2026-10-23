package dev.svarcovv.hiderflags.item;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.block.BlockPredicate;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAdventurePredicate;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.ShownInTooltip;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ColorableArmorMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public final class ItemHiderServerChecks extends JavaPlugin {

    private static final List<DataComponentType.Valued<?>> TOOLTIP_COMPONENTS = List.of(
            DataComponentTypes.ATTRIBUTE_MODIFIERS, DataComponentTypes.ENCHANTMENTS,
            DataComponentTypes.STORED_ENCHANTMENTS, DataComponentTypes.UNBREAKABLE,
            DataComponentTypes.DYED_COLOR, DataComponentTypes.TRIM, DataComponentTypes.CAN_BREAK,
            DataComponentTypes.CAN_PLACE_ON, DataComponentTypes.JUKEBOX_PLAYABLE);

    private ItemHider hider;
    private Plugin plugin;
    private int assertions;
    private int materials;

    @Override
    public void onEnable() {
        plugin = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("HiderFlags"));
        hider = new ItemHider(plugin);
        Bukkit.getScheduler().runTask(this, this::runChecks);
    }

    private void runChecks() {
        String result;
        try {
            check(!hider.hide(null), "null item");
            check(!hider.hide(ItemStack.empty()), "empty item");
            check(!hider.isHidden(null), "null ownership");
            check(!hider.isHidden(ItemStack.empty()), "empty ownership");
            for (Material material : Material.values()) {
                if (!material.isLegacy() && material.isItem() && !material.isAir()) {
                    verifyItem(ItemStack.of(material));
                    materials++;
                }
            }
            checkPotions();
            checkEnchantedBook();
            checkDecoratedArmor();
            checkCustomItem();
            checkLegacyItems();
            checkExplicitEmptyAttributes();
            checkInventory();
            checkRefresh();
            checkAdventurePredicates();
            checkMaterialUpgrade();
            result = "PASS: " + assertions + " assertions, " + materials + " item types";
            getLogger().info(result);
        } catch (Throwable failure) {
            result = "FAIL: " + failure;
            getLogger().log(Level.SEVERE, "Server checks failed", failure);
        }
        try {
            Files.createDirectories(getDataFolder().toPath());
            Files.writeString(getDataFolder().toPath().resolve("result.txt"), result);
        } catch (Exception failure) {
            getLogger().log(Level.SEVERE, "Cannot save result", failure);
        } finally {
            Bukkit.shutdown();
        }
    }

    private void verifyItem(ItemStack item) {
        ItemStack original = item.clone();
        check(hider.hide(item), "first hide: " + item.getType());
        check(hider.isHidden(item), "ownership: " + item.getType());
        check(item.hasData(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP), "additional tooltip: " + item.getType());
        for (DataComponentType.Valued<?> type : TOOLTIP_COMPONENTS) {
            verifyComponent(original, item, type);
        }
        check(item.getAmount() == original.getAmount(), "stack size");
        check(Objects.equals(original.getData(DataComponentTypes.POTION_CONTENTS),
                item.getData(DataComponentTypes.POTION_CONTENTS)), "potion contents");
        check(Objects.equals(original.getData(DataComponentTypes.DAMAGE),
                item.getData(DataComponentTypes.DAMAGE)), "durability");
        byte[] hidden = item.serializeAsBytes();
        check(!hider.hide(item), "second hide: " + item.getType());
        check(Arrays.equals(hidden, item.serializeAsBytes()), "idempotent bytes: " + item.getType());
        ItemStack restored = ItemStack.deserializeBytes(hidden);
        check(hider.isHidden(restored), "saved ownership: " + item.getType());
        check(item.getType() == restored.getType() && item.getAmount() == restored.getAmount()
                && item.getItemMeta().getAsString().equals(restored.getItemMeta().getAsString()),
                "saved metadata: " + item.getType());
    }

    private <T> void verifyComponent(ItemStack original, ItemStack item, DataComponentType.Valued<T> type) {
        T before = original.getData(type);
        T after = item.getData(type);
        if (before == null) {
            check(after == null, "absent component: " + type + " on " + item.getType());
        } else if (before instanceof ShownInTooltip<?> shownBefore && after instanceof ShownInTooltip<?> shownAfter) {
            check(!shownAfter.showInTooltip(), "hidden component: " + type + " on " + item.getType());
            check(shownAfter.showInTooltip(shownBefore.showInTooltip()).equals(before),
                    "preserved component: " + type + " on " + item.getType());
        } else {
            throw new AssertionError("Unexpected tooltip component: " + type);
        }
    }

    private void checkPotions() {
        for (Material material : List.of(Material.POTION, Material.SPLASH_POTION,
                Material.LINGERING_POTION, Material.TIPPED_ARROW)) {
            ItemStack item = ItemStack.of(material, material == Material.TIPPED_ARROW ? 32 : 1);
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            meta.setBasePotionType(PotionType.LONG_STRENGTH);
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1200, 2), true);
            meta.setColor(Color.PURPLE);
            item.setItemMeta(meta);
            verifyItem(item);
            check(item.hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP), "potion tooltip flag");
        }
    }

    private void checkEnchantedBook() {
        ItemStack book = ItemStack.of(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.addStoredEnchant(Enchantment.SHARPNESS, 5, false);
        meta.addStoredEnchant(Enchantment.UNBREAKING, 3, false);
        book.setItemMeta(meta);
        verifyItem(book);
        check(book.hasItemFlag(ItemFlag.HIDE_STORED_ENCHANTS), "stored enchantments hidden");
        check(((EnchantmentStorageMeta) book.getItemMeta()).getStoredEnchantLevel(Enchantment.SHARPNESS) == 5,
                "stored enchantment level preserved");
    }

    private void checkDecoratedArmor() {
        ItemStack armor = ItemStack.of(Material.LEATHER_CHESTPLATE);
        ColorableArmorMeta meta = (ColorableArmorMeta) armor.getItemMeta();
        meta.setColor(Color.ORANGE);
        meta.setTrim(new ArmorTrim(TrimMaterial.AMETHYST, TrimPattern.SENTRY));
        armor.setItemMeta(meta);
        verifyItem(armor);
        check(armor.hasItemFlag(ItemFlag.HIDE_DYE), "dye tooltip");
        check(armor.hasItemFlag(ItemFlag.HIDE_ARMOR_TRIM), "trim tooltip");
    }

    private void checkCustomItem() {
        ItemStack sword = ItemStack.of(Material.DIAMOND_SWORD);
        Damageable meta = (Damageable) sword.getItemMeta();
        meta.setDamage(127);
        meta.displayName(Component.text("Клинок"));
        meta.lore(List.of(Component.text("Описание предмета")));
        meta.setUnbreakable(true);
        meta.addEnchant(Enchantment.SHARPNESS, 5, false);
        meta.getPersistentDataContainer().set(new NamespacedKey("other", "data"), PersistentDataType.STRING, "value");
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                new NamespacedKey("other", "damage"), 12, AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND));
        sword.setItemMeta(meta);
        ItemMeta before = sword.getItemMeta();
        verifyItem(sword);
        ItemMeta after = sword.getItemMeta();
        check(Objects.equals(before.displayName(), after.displayName()), "custom name");
        check(Objects.equals(before.lore(), after.lore()), "lore");
        check("value".equals(after.getPersistentDataContainer().get(new NamespacedKey("other", "data"),
                PersistentDataType.STRING)), "foreign persistent data");
        check(after.hasItemFlag(ItemFlag.HIDE_ENCHANTS), "enchant tooltip");
        check(after.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE), "unbreakable tooltip");
    }

    private void checkLegacyItems() {
        for (Material material : List.of(Material.DIAMOND_SWORD, Material.NETHERITE_CHESTPLATE,
                Material.TIPPED_ARROW, Material.STONE)) {
            ItemStack item = ItemStack.of(material);
            ItemAttributeModifiers original = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes()
                    .addModifier(Attribute.ATTACK_SPEED, legacyModifier()).showInTooltip(false));
            check(hider.isHidden(item), "legacy ownership");
            check(hider.hide(item), "legacy repair");
            ItemAttributeModifiers repaired = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            check(Objects.equals(original == null ? null : original.modifiers(),
                    repaired == null ? null : repaired.modifiers()), "restored defaults: " + material);
            check(!hider.hide(item), "legacy repair idempotent");
        }
        ItemStack mixed = ItemStack.of(Material.DIAMOND_AXE);
        AttributeModifier custom = new AttributeModifier(new NamespacedKey("other", "speed"),
                0.5, AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.MAINHAND);
        mixed.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes()
                .addModifier(Attribute.ATTACK_SPEED, legacyModifier()).addModifier(Attribute.ATTACK_SPEED, custom));
        hider.hide(mixed);
        List<ItemAttributeModifiers.Entry> remaining = mixed.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers();
        check(remaining.size() == 1 && remaining.getFirst().modifier().equals(custom), "legacy keeps custom modifier");
        ItemStack foreign = ItemStack.of(Material.DIAMOND_AXE);
        foreign.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes()
                .addModifier(Attribute.ATTACK_SPEED, new AttributeModifier(
                        new NamespacedKey("other", "dummy_attribute"), 0,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY)));
        verifyItem(foreign);
    }

    private AttributeModifier legacyModifier() {
        return new AttributeModifier(new NamespacedKey(plugin, "dummy_attribute"), 0,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY);
    }

    private void checkExplicitEmptyAttributes() {
        ItemStack item = ItemStack.of(Material.DIAMOND_SWORD);
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes());
        verifyItem(item);
        check(item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().isEmpty(), "intentional empty attributes");
    }

    private void checkInventory() {
        Inventory inventory = Bukkit.createInventory(null, 45);
        for (int slot : List.of(0, 35, 36, 37, 38, 39, 40)) {
            inventory.setItem(slot, ItemStack.of(slot >= 36 ? Material.DIAMOND_HELMET : Material.STONE));
        }
        check(hider.hideInventory(inventory) == 7, "inventory includes equipment slots");
        for (int slot : List.of(0, 35, 36, 37, 38, 39, 40)) {
            check(hider.isHidden(inventory.getItem(slot)), "inventory slot: " + slot);
        }
        check(hider.hideInventory(inventory) == 0, "inventory changed count");
    }

    private void checkRefresh() {
        Inventory inventory = Bukkit.createInventory(null, 9);
        ItemStack hidden = ItemStack.of(Material.DIAMOND_SWORD);
        hider.hide(hidden);
        hidden.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        hidden.setData(DataComponentTypes.ENCHANTMENTS,
                hidden.getData(DataComponentTypes.ENCHANTMENTS).showInTooltip(true));
        hidden.unsetData(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP);
        inventory.setItem(0, hidden);
        ItemStack foreign = ItemStack.of(Material.DIAMOND_AXE);
        foreign.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        byte[] untouched = foreign.serializeAsBytes();
        inventory.setItem(1, foreign);
        hider.refresh(inventory);
        check(inventory.getItem(0).hasItemFlag(ItemFlag.HIDE_ENCHANTS), "refresh new enchantment");
        check(inventory.getItem(0).hasItemFlag(ItemFlag.HIDE_ADDITIONAL_TOOLTIP), "refresh additional tooltip");
        check(Arrays.equals(untouched, inventory.getItem(1).serializeAsBytes()), "foreign hidden item untouched");
    }

    private void checkAdventurePredicates() {
        ItemStack item = ItemStack.of(Material.DIAMOND_PICKAXE);
        item.setData(DataComponentTypes.CAN_BREAK, ItemAdventurePredicate.itemAdventurePredicate()
                .addPredicate(BlockPredicate.predicate().build()));
        item.setData(DataComponentTypes.CAN_PLACE_ON, ItemAdventurePredicate.itemAdventurePredicate()
                .addPredicate(BlockPredicate.predicate().build()));
        verifyItem(item);
        check(item.hasItemFlag(ItemFlag.HIDE_DESTROYS), "adventure break tooltip");
        check(item.hasItemFlag(ItemFlag.HIDE_PLACED_ON), "adventure place tooltip");
    }

    private void checkMaterialUpgrade() {
        for (Material material : List.of(Material.DIAMOND_SWORD, Material.DIAMOND_AXE,
                Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS)) {
            ItemStack base = ItemStack.of(material);
            hider.hide(base);
            Material upgraded = Material.valueOf(material.name().replace("DIAMOND", "NETHERITE"));
            ItemStack result = base.withType(upgraded);
            hider.hide(result);
            check(result.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().equals(
                    upgraded.getDefaultData(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers()),
                    "upgraded default attributes: " + upgraded);
            check(!hider.hide(result), "upgraded idempotence");
        }
        ItemStack custom = ItemStack.of(Material.DIAMOND_SWORD);
        hider.hide(custom);
        custom.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.itemAttributes()
                .addModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey("other", "custom"),
                        25, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND)));
        ItemAttributeModifiers before = custom.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        ItemStack upgraded = custom.withType(Material.NETHERITE_SWORD);
        hider.hide(upgraded);
        check(upgraded.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS).modifiers().equals(before.modifiers()),
                "upgraded custom attributes preserved");
    }

    private void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
