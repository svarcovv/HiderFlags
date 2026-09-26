package org.exampleD.hiderFlags;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class HiderFlags extends JavaPlugin implements CommandExecutor, Listener {

    private NamespacedKey dummyKey;
    private static String PREFIX = "§6☠ | §f";

    @Override
    public void onEnable() {
        this.dummyKey = new NamespacedKey(this, "dummy_attribute");

        if (getCommand("hidehand") != null) getCommand("hidehand").setExecutor(this);
        if (getCommand("hideinv") != null) getCommand("hideinv").setExecutor(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может выполнять только игрок");
            return true;
        }

        if (command.getName().equalsIgnoreCase("hidehand")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                player.sendMessage(PREFIX + "Вы должны держать предмет в руке");
                return true;
            }

            hideOnItem(item);
            player.updateInventory();
            player.sendMessage(PREFIX + "Флаги предмета успешно скрыты");
            return true;
        }

        if (command.getName().equalsIgnoreCase("hideinv")) {
            int changedCount = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    hideOnItem(item);
                    changedCount++;
                }
            }
            player.updateInventory();
            player.sendMessage(PREFIX + "Флаги скрыты у §6" + changedCount + " §fпредметов");
            return true;
        }

        return false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
            if (hasHiderEnabled(event.getCurrentItem())) {
                hideOnItem(event.getCurrentItem());
            }
        }
        if (event.getCursor().getType() != Material.AIR) {
            if (hasHiderEnabled(event.getCursor())) {
                hideOnItem(event.getCursor());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void hideOnItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (!meta.hasAttributeModifiers()) {
            AttributeModifier dummyModifier = new AttributeModifier(
                    dummyKey,
                    0.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.ANY
            );
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, dummyModifier);
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        meta.addItemFlags(ItemFlag.HIDE_DYE);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        item.setItemMeta(meta);
    }

    private boolean hasHiderEnabled(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES);
    }
}