package dev.svarcovv.hiderflags;

import dev.svarcovv.hiderflags.command.HideCommand;
import dev.svarcovv.hiderflags.item.HiddenItemListener;
import dev.svarcovv.hiderflags.item.ItemHider;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;

public final class HiderFlags extends JavaPlugin {

    @Override
    public void onEnable() {
        ItemHider itemHider = new ItemHider(this);
        HideCommand executor = new HideCommand(itemHider);
        Objects.requireNonNull(getCommand("hidehand")).setExecutor(executor);
        Objects.requireNonNull(getCommand("hideinv")).setExecutor(executor);
        getServer().getPluginManager().registerEvents(new HiddenItemListener(this, itemHider), this);
        getServer().getOnlinePlayers().forEach(player -> itemHider.refresh(player.getInventory()));
    }
}
