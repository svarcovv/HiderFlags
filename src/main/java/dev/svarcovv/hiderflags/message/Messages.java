package dev.svarcovv.hiderflags.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public final class Messages {

    private static final Component PREFIX = Component.text("☠ | ", NamedTextColor.GOLD);

    private Messages() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX.append(Component.text(message, NamedTextColor.WHITE)));
    }

    public static void hiddenItems(CommandSender sender, int count) {
        sender.sendMessage(PREFIX
                .append(Component.text("Флаги скрыты у ", NamedTextColor.WHITE))
                .append(Component.text(count, NamedTextColor.GOLD))
                .append(Component.text(" предметов", NamedTextColor.WHITE)));
    }
}
