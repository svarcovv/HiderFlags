package dev.svarcovv.hiderflags.item;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.ShownInTooltip;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public final class ItemHider {

    private final NamespacedKey hiddenKey;
    private final NamespacedKey legacyDummyKey;
    private final NamespacedKey attributeSourceKey;

    public ItemHider(Plugin plugin) {
        hiddenKey = new NamespacedKey(plugin, "hidden");
        legacyDummyKey = new NamespacedKey(plugin, "dummy_attribute");
        attributeSourceKey = new NamespacedKey(plugin, "attribute_source");
    }

    public boolean hide(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        boolean changed = repairLegacyAttributes(item);
        changed |= updateDefaultAttributes(item);
        changed |= hideComponent(item, DataComponentTypes.ATTRIBUTE_MODIFIERS);
        changed |= hideComponent(item, DataComponentTypes.ENCHANTMENTS);
        changed |= hideComponent(item, DataComponentTypes.STORED_ENCHANTMENTS);
        changed |= hideComponent(item, DataComponentTypes.UNBREAKABLE);
        changed |= hideComponent(item, DataComponentTypes.DYED_COLOR);
        changed |= hideComponent(item, DataComponentTypes.TRIM);
        changed |= hideComponent(item, DataComponentTypes.CAN_BREAK);
        changed |= hideComponent(item, DataComponentTypes.CAN_PLACE_ON);
        changed |= hideComponent(item, DataComponentTypes.JUKEBOX_PLAYABLE);
        if (!item.hasData(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP)) {
            item.setData(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP);
            changed = true;
        }
        if (!item.getPersistentDataContainer().has(hiddenKey, PersistentDataType.BYTE)) {
            item.editPersistentDataContainer(data -> data.set(hiddenKey, PersistentDataType.BYTE, (byte) 1));
            changed = true;
        }
        return changed;
    }

    public boolean isHidden(@Nullable ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }
        return item.getPersistentDataContainer().has(hiddenKey, PersistentDataType.BYTE)
                || hasLegacyDummy(item);
    }

    public int hideInventory(Inventory inventory) {
        int changed = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (hide(item)) {
                inventory.setItem(slot, item);
                changed++;
            }
        }
        return changed;
    }

    public void refresh(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isHidden(item) && hide(item)) {
                inventory.setItem(slot, item);
            }
        }
    }

    private <T extends ShownInTooltip<T>> boolean hideComponent(ItemStack item, DataComponentType.Valued<T> type) {
        T component = item.getData(type);
        if (component == null || !component.showInTooltip()) {
            return false;
        }
        item.setData(type, component.showInTooltip(false));
        return true;
    }

    private boolean hasLegacyDummy(ItemStack item) {
        ItemAttributeModifiers attributes = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        return attributes != null && attributes.modifiers().stream().anyMatch(this::isLegacyDummy);
    }

    private boolean updateDefaultAttributes(ItemStack item) {
        String source = item.getPersistentDataContainer().get(attributeSourceKey, PersistentDataType.STRING);
        ItemAttributeModifiers attributes = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        boolean changed = false;
        if (source != null) {
            Material previous = Material.matchMaterial(source);
            if (previous != null && previous != item.getType()
                    && sameModifiers(attributes, previous.getDefaultData(DataComponentTypes.ATTRIBUTE_MODIFIERS))) {
                item.resetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                attributes = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
                changed = true;
            }
        }
        ItemAttributeModifiers defaults = item.getType().getDefaultData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributes != null && !attributes.modifiers().isEmpty() && sameModifiers(attributes, defaults)) {
            String current = item.getType().name();
            if (!current.equals(source)) {
                item.editPersistentDataContainer(data -> data.set(attributeSourceKey, PersistentDataType.STRING, current));
                changed = true;
            }
        } else if (source != null) {
            item.editPersistentDataContainer(data -> data.remove(attributeSourceKey));
            changed = true;
        }
        return changed;
    }

    private boolean sameModifiers(@Nullable ItemAttributeModifiers first, @Nullable ItemAttributeModifiers second) {
        return first != null && second != null && first.modifiers().equals(second.modifiers());
    }

    private boolean repairLegacyAttributes(ItemStack item) {
        ItemAttributeModifiers attributes = item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (attributes == null || attributes.modifiers().stream().noneMatch(this::isLegacyDummy)) {
            return false;
        }
        List<ItemAttributeModifiers.Entry> remaining = attributes.modifiers().stream()
                .filter(entry -> !isLegacyDummy(entry)).toList();
        if (remaining.isEmpty()) {
            item.resetData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        } else {
            ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.itemAttributes();
            remaining.forEach(entry -> builder.addModifier(entry.attribute(), entry.modifier(), entry.getGroup()));
            item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.showInTooltip(false));
        }
        return true;
    }

    private boolean isLegacyDummy(ItemAttributeModifiers.Entry entry) {
        AttributeModifier modifier = entry.modifier();
        return entry.attribute().equals(Attribute.ATTACK_SPEED)
                && modifier.getKey().equals(legacyDummyKey)
                && modifier.getAmount() == 0.0
                && modifier.getOperation() == AttributeModifier.Operation.ADD_NUMBER
                && entry.getGroup().equals(EquipmentSlotGroup.ANY);
    }
}
