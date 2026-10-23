package dev.svarcovv.hiderflags.command;

import dev.svarcovv.hiderflags.item.ItemHider;
import dev.svarcovv.hiderflags.message.Messages;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class HideCommand implements TabExecutor {

    private final ItemHider itemHider;

    public HideCommand(ItemHider itemHider) {
        this.itemHider = itemHider;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "Эту команду может выполнять только игрок");
            return true;
        }
        if (!player.hasPermission("hiderflags.use")) {
            Messages.send(player, "У вас нет прав на выполнение этой команды");
            return true;
        }
        if (args.length != 0) {
            Messages.send(player, "Использование: /" + label);
            return true;
        }
        switch (command.getName()) {
            case "hidehand" -> {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.isEmpty()) {
                    Messages.send(player, "Вы должны держать предмет в руке");
                    return true;
                }
                itemHider.hide(item);
                player.getInventory().setItemInMainHand(item);
                Messages.send(player, "Флаги предмета успешно скрыты");
            }
            case "hideinv" -> Messages.hiddenItems(player, itemHider.hideInventory(player.getInventory()));
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                              @NotNull String alias, @NotNull String[] args) {
        return List.of();
    }
}
